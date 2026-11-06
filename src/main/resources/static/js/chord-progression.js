(() => {
  const progression = document.getElementById("chord-progression");
  if (!progression) return;

  let chords = Array.from(progression.querySelectorAll(".chord-progression-chord"));
  const toggleButton = document.getElementById("toggle-progression");
  const playIcon = toggleButton.querySelector(".play-icon");
  const stopIcon = toggleButton.querySelector(".stop-icon");
  const tempoInput = document.getElementById("progression-tempo");
  const scaleType = document.getElementById("scale-type");
  const progressionType = document.getElementById("progression-type");
  const scaleDisplay = document.getElementById("scale-display");
  const status = document.getElementById("progression-status");
  const pitchClasses = { C: 0, "C#": 1, Db: 1, D: 2, "D#": 3, Eb: 3, E: 4, F: 5,
    "F#": 6, Gb: 6, G: 7, "G#": 8, Ab: 8, A: 9, "A#": 10, Bb: 10, B: 11 };
  const sampleNames = ["C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B"];

  let audioContext;
  let masterGain;
  let pianoSamplesPromise;
  let playbackTimer;
  const activeSources = new Map();
  let playing = false;
  let playbackGeneration = 0;

  function setActiveChord(index) {
    chords.forEach((chord, chordIndex) => {
      chord.classList.toggle("text-primary", chordIndex === index);
      chord.classList.toggle("fw-bold", chordIndex === index);
    });
  }

  function renderChords(sequence) {
    const chordList = progression.querySelector("ul");
    chordList.replaceChildren(...sequence.split("|").map((name) => {
      const chord = document.createElement("li");
      chord.className = "list-inline-item chord-progression-chord";
      chord.textContent = name;
      return chord;
    }));
    chords = Array.from(chordList.querySelectorAll(".chord-progression-chord"));
  }

  function stopPlayback(message = "Playback stopped.") {
    playbackGeneration++;
    window.clearTimeout(playbackTimer);
    const now = audioContext?.currentTime ?? 0;
    activeSources.forEach((gain, source) => {
      try {
        gain.gain.cancelScheduledValues(now);
        gain.gain.setValueAtTime(Math.max(gain.gain.value, 0.0001), now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.04);
        source.stop(now + 0.05);
      } catch {
        // The sample may already have stopped.
      }
    });
    activeSources.clear();
    playing = false;
    playIcon.classList.remove("d-none");
    stopIcon.classList.add("d-none");
    toggleButton.setAttribute("aria-label", "Play backing track");
    toggleButton.title = "Play backing track";
    toggleButton.setAttribute("aria-pressed", "false");
    setActiveChord(-1);
    status.textContent = message;
  }

  function loadPianoSamples() {
    if (!pianoSamplesPromise) {
      pianoSamplesPromise = Promise.all(sampleNames.map(async (sampleName) => {
        const response = await fetch(`/audio/piano/${sampleName}4.mp3`);
        if (!response.ok) {
          throw new Error(`Could not load piano sample ${sampleName}4.`);
        }
        return audioContext.decodeAudioData(await response.arrayBuffer());
      }));
    }
    return pianoSamplesPromise;
  }

  function chordNotes(chord) {
    const match = chord.textContent.trim().match(/^([A-G](?:#|b)?)(m7b5|m7|m|7|dim)?$/);
    if (!match || pitchClasses[match[1]] === undefined) {
      throw new Error(`Unsupported chord: ${chord.textContent.trim()}`);
    }

    const root = pitchClasses[match[1]];
    const intervals = match[2] === "m" ? [0, 3, 7]
      : match[2] === "m7" ? [0, 3, 7, 10]
        : match[2] === "m7b5" ? [0, 3, 6, 10]
          : match[2] === "dim" ? [0, 3, 6]
            : match[2] === "7" ? [0, 4, 7, 10] : [0, 4, 7];
    return [root + 48, ...intervals.map((interval) => root + 60 + interval)];
  }

  function playChord(index, chordDuration, samples, barStart) {
    if (!playing) return;
    index %= chords.length;

    const chord = chords[index];
    setActiveChord(index);
    status.textContent = `Backing track: ${chord.textContent.trim()} (${index + 1} of ${chords.length})`;
    const notes = chordNotes(chord);
    const [bassNote] = notes;
    const beatDuration = chordDuration / 4;
    const chordVoicing = notes.length === 5 ? notes.slice(1) : [...notes.slice(1), bassNote + 24];
    const scheduleNote = (midiNote, startOffset, velocity, strum = 0) => {
      const pitchClass = midiNote % 12;
      const sourceMidiNote = 60 + pitchClass;
      const source = audioContext.createBufferSource();
      const gain = audioContext.createGain();
      const startTime = barStart + startOffset + strum;
      source.buffer = samples[pitchClass];
      source.playbackRate.value = 2 ** ((midiNote - sourceMidiNote) / 12);
      gain.gain.setValueAtTime(0.0001, startTime);
      gain.gain.exponentialRampToValueAtTime(velocity, startTime + 0.012);
      source.connect(gain);
      gain.connect(masterGain);
      source.start(startTime);
      source.addEventListener("ended", () => activeSources.delete(source), { once: true });
      activeSources.set(source, gain);
    };

    const strumChord = (startOffset, velocity, spread = 0.008) => {
      chordVoicing.forEach((note, noteIndex) => {
        scheduleNote(note, startOffset, velocity * (0.96 + Math.random() * 0.08),
          noteIndex * (spread + Math.random() * 0.005));
      });
    };

    for (let beat = 0; beat < 4; beat++) {
      const downbeat = beat === 0;
      strumChord(beat * beatDuration, downbeat ? 0.2 : 0.13, downbeat ? 0.005 : 0.008);
      if (beat === 0) {
        scheduleNote(bassNote, 0, 0.29);
      } else if (beat === 2) {
        scheduleNote(bassNote + 7, beat * beatDuration, 0.25);
      }
    }

    playbackTimer = window.setTimeout(
      () => playChord(index + 1, chordDuration, samples, barStart + chordDuration),
      Math.max(0, (barStart + chordDuration - audioContext.currentTime - 0.1) * 1000)
    );
  }

  toggleButton.addEventListener("click", async () => {
    if (playing) {
      stopPlayback();
      return;
    }

    if (!window.AudioContext) {
      status.textContent = "Audio playback is not supported in this browser.";
      return;
    }

    const tempo = Number(tempoInput.value);
    if (!Number.isFinite(tempo) || tempo < 40 || tempo > 200) {
      status.textContent = "Enter a tempo between 40 and 200 BPM.";
      tempoInput.focus();
      return;
    }

    const generation = ++playbackGeneration;
    try {
      audioContext ??= new AudioContext();
      masterGain ??= audioContext.createGain();
      masterGain.gain.setValueAtTime(0.82, audioContext.currentTime);
      masterGain.connect(audioContext.destination);
      await audioContext.resume();
      if (generation !== playbackGeneration) return;
      playing = true;
      playIcon.classList.add("d-none");
      stopIcon.classList.remove("d-none");
      toggleButton.setAttribute("aria-label", "Stop backing track");
      toggleButton.title = "Stop backing track";
      toggleButton.setAttribute("aria-pressed", "true");
      status.textContent = "Loading piano...";
      const samples = await loadPianoSamples();
      if (playing && generation === playbackGeneration) {
        status.textContent = "Starting progression...";
        playChord(0, 240 / tempo, samples, audioContext.currentTime + 0.05);
      }
    } catch (error) {
      if (generation === playbackGeneration) {
        stopPlayback("Unable to load piano audio. Please try again.");
        pianoSamplesPromise = undefined;
      }
      console.error("Unable to play the piano chord progression.", error);
    }
  });

  progressionType.addEventListener("change", () => {
    if (playing) stopPlayback("Progression changed. Press Play to start the new backing track.");
    renderChords(progressionType.selectedOptions[0].dataset.chords);
    setActiveChord(-1);
  });

  scaleType.addEventListener("change", () => {
    if (playing) stopPlayback("Scale changed. Press Play to start the new backing track.");
    scaleDisplay.textContent = scaleType.selectedOptions[0].textContent;
    progressionType.querySelectorAll("option").forEach((option) => {
      option.dataset.chords = option.dataset.chordsByScale.split(";")[scaleType.selectedIndex];
    });
    renderChords(progressionType.selectedOptions[0].dataset.chords);
    setActiveChord(-1);
  });
})();
