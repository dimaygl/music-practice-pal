(() => {
  const progression = document.getElementById("chord-progression");
  if (!progression) return;

  const progressionData = JSON.parse(progression.dataset.progressions);
  const chordList = progression.querySelector("ul");
  const toggleButton = document.getElementById("toggle-progression");
  const playIcon = toggleButton.querySelector(".play-icon");
  const stopIcon = toggleButton.querySelector(".stop-icon");
  const tempoInput = document.getElementById("progression-tempo");
  const scaleType = document.getElementById("scale-type");
  const progressionType = document.getElementById("progression-type");
  const scaleDisplay = document.getElementById("scale-display");
  const status = document.getElementById("progression-status");

  const LETTER_PITCHES = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
  const ACCIDENTALS = { "": 0, "#": 1, "##": 2, b: -1, bb: -2 };
  const CHORD_INTERVALS = {
    "": [0, 4, 7], m: [0, 3, 7], 7: [0, 4, 7, 10], maj7: [0, 4, 7, 11], m7: [0, 3, 7, 10],
    dim: [0, 3, 6], dim7: [0, 3, 6, 9], m7b5: [0, 3, 6, 10],
  };
  // One sample every minor third from C2 to A5, so no note is pitch-shifted more than a semitone.
  const SAMPLES = [2, 3, 4, 5].flatMap((octave) =>
    [["C", 0], ["Ds", 3], ["Fs", 6], ["A", 9]].map(([name, offset]) =>
      ({ file: `${name}${octave}`, midi: 12 * (octave + 1) + offset })));
  const MIN_TEMPO = 40;
  const MAX_TEMPO = 200;
  // Schedule well ahead of the audio clock so throttled timers (background tabs clamp to ~1s)
  // don't starve playback; the scheduler tick only needs to run often enough to refill the window.
  const SCHEDULE_AHEAD_SECONDS = 1.5;
  const SCHEDULER_INTERVAL_MS = 50;
  const START_DELAY_SECONDS = 0.05;

  // Piano sound
  const DAMPER_SECONDS = 0.09; // time constant of the damper falling on a released key
  const PEDAL_OVERLAP_SECONDS = 0.08; // pedal is lifted just after the next chord is struck
  const HOLD = Infinity; // note duration meaning "until the pedal is lifted at the end of the bar"
  const REVERB_SECONDS = 2.4;
  const REVERB_LEVEL = 0.24;

  // Voicing ranges (MIDI note numbers)
  const VOICING_LOW = 52; // E3
  const VOICING_HIGH = 76; // E5
  const VOICING_CENTRE = 64;
  const BASS_LOW = 36; // C2
  const BASS_HIGH = 50; // D3
  const BASS_CENTRE = 43;

  let chords = [];
  let style = "ballad";
  let audioContext;
  let pianoBus;
  let pianoSamplesPromise;
  let samples;
  let schedulerTimer;
  const activeSources = new Map();
  const soundingNotes = new Map();
  let playing = false;
  let playbackGeneration = 0;
  let tempo = readTempo() ?? 80;
  let nextBarIndex = 0;
  let nextBarStart = 0;
  let barsPlayed = 0;
  let previousVoicing;
  let previousBass;
  let scheduledBars = [];
  let displayedBar;

  const random = (min, max) => min + Math.random() * (max - min);
  const humanize = (seconds = 0.007) => random(-seconds, seconds);
  const pitchClass = (note) => ((note % 12) + 12) % 12;

  function isMinorKey(key) {
    return key.endsWith(" minor");
  }

  function readTempo() {
    const value = Number(tempoInput.value);
    return Number.isFinite(value) && value >= MIN_TEMPO && value <= MAX_TEMPO ? value : null;
  }

  function parseChord(name) {
    const match = name.match(/^([A-G])(##|#|bb|b)?(maj7|m7b5|m7|m|7|dim7|dim)?$/);
    if (!match) throw new Error(`Unsupported chord: ${name}`);
    const root = pitchClass(LETTER_PITCHES[match[1]] + ACCIDENTALS[match[2] ?? ""]);
    return { name, root, intervals: CHORD_INTERVALS[match[3] ?? ""] };
  }

  function setActiveChord(index) {
    chords.forEach((chord, chordIndex) => {
      chord.element.classList.toggle("text-primary", chordIndex === index);
      chord.element.classList.toggle("fw-bold", chordIndex === index);
    });
  }

  function renderChords(sequence) {
    chords = sequence.split("|").map((name) => {
      const element = document.createElement("li");
      element.className = "list-inline-item chord-progression-chord";
      element.textContent = name;
      return { element, ...parseChord(name) };
    });
    chordList.replaceChildren(...chords.map((chord) => chord.element));
  }

  // Bring the labels, scale display and chord list in line with the current control values.
  // Runs on load too, because browsers may restore <select> values without firing "change".
  function syncControls() {
    const key = scaleType.value;
    const minor = isMinorKey(key);
    scaleDisplay.textContent = scaleType.selectedOptions[0].textContent;
    for (const option of progressionType.options) {
      const data = progressionData[option.value];
      option.textContent = minor ? data.minorLabel : data.majorLabel;
    }
    const selected = progressionData[progressionType.value];
    style = selected.style;
    renderChords(selected.chords[key]);
    setActiveChord(-1);
  }

  function setPlayingUi(isPlaying) {
    playIcon.classList.toggle("d-none", isPlaying);
    stopIcon.classList.toggle("d-none", !isPlaying);
    const label = isPlaying ? "Stop backing track" : "Play backing track";
    toggleButton.setAttribute("aria-label", label);
    toggleButton.title = label;
    toggleButton.setAttribute("aria-pressed", String(isPlaying));
  }

  function stopPlayback(message = "Playback stopped.") {
    playbackGeneration++;
    window.clearInterval(schedulerTimer);
    schedulerTimer = undefined;
    const now = audioContext?.currentTime ?? 0;
    activeSources.forEach((gain, source) => {
      try {
        gain.gain.cancelScheduledValues(now);
        gain.gain.setValueAtTime(Math.max(gain.gain.value, 0.0001), now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.08);
        source.stop(now + 0.1);
      } catch {
        // The sample may already have stopped.
      }
    });
    activeSources.clear();
    soundingNotes.clear();
    scheduledBars = [];
    displayedBar = undefined;
    playing = false;
    setPlayingUi(false);
    setActiveChord(-1);
    status.textContent = message;
  }

  // A short, dark stereo room: decaying noise that loses high frequencies as it fades.
  function createImpulseResponse(context) {
    const rate = context.sampleRate;
    const length = Math.floor(rate * REVERB_SECONDS);
    const preDelay = Math.floor(rate * 0.015);
    const impulse = context.createBuffer(2, length, rate);
    for (let channel = 0; channel < 2; channel++) {
      const data = impulse.getChannelData(channel);
      let smoothed = 0;
      for (let i = preDelay; i < length; i++) {
        const progress = (i - preDelay) / (length - preDelay);
        smoothed += (random(-1, 1) - smoothed) * (0.55 - 0.45 * progress);
        data[i] = smoothed * (1 - progress) ** 3.5;
      }
    }
    return impulse;
  }

  function ensureAudio() {
    if (audioContext) return;
    audioContext = new AudioContext();

    const compressor = audioContext.createDynamicsCompressor();
    compressor.threshold.value = -16;
    compressor.knee.value = 10;
    compressor.ratio.value = 3;
    compressor.attack.value = 0.01;
    compressor.release.value = 0.25;

    const rumbleFilter = audioContext.createBiquadFilter();
    rumbleFilter.type = "highpass";
    rumbleFilter.frequency.value = 38;

    const master = audioContext.createGain();
    master.gain.value = 0.8;
    master.connect(rumbleFilter).connect(compressor).connect(audioContext.destination);

    pianoBus = audioContext.createGain();
    const reverb = audioContext.createConvolver();
    reverb.buffer = createImpulseResponse(audioContext);
    const wet = audioContext.createGain();
    wet.gain.value = REVERB_LEVEL;
    pianoBus.connect(master);
    pianoBus.connect(reverb).connect(wet).connect(master);
  }

  function loadPianoSamples() {
    pianoSamplesPromise ??= Promise.all(SAMPLES.map(async (sample) => {
      const response = await fetch(`/audio/piano/${sample.file}.mp3`);
      if (!response.ok) {
        throw new Error(`Could not load piano sample ${sample.file}.`);
      }
      return { midi: sample.midi, buffer: await audioContext.decodeAudioData(await response.arrayBuffer()) };
    })).catch((error) => {
      // Allow the next Play press to retry instead of reusing the rejected promise.
      pianoSamplesPromise = undefined;
      throw error;
    });
    return pianoSamplesPromise;
  }

  function nearestSample(note) {
    return samples.reduce((best, sample) =>
      Math.abs(sample.midi - note) < Math.abs(best.midi - note) ? sample : best);
  }

  // Plays one piano key. Velocity (0–1) controls both loudness and brightness, like a real hammer.
  function playNote(note, startTime, duration, velocity) {
    const other = soundingNotes.get(note);
    // Both hands striking the same key together: it can only sound once.
    if (other && Math.abs(other.startTime - startTime) < 0.02) return;

    const sample = nearestSample(note);
    const source = audioContext.createBufferSource();
    source.buffer = sample.buffer;
    source.playbackRate.value = 2 ** ((note - sample.midi) / 12);

    const tone = audioContext.createBiquadFilter();
    tone.type = "lowpass";
    tone.frequency.value = Math.min(15000, 900 + velocity * 9000 + Math.max(0, note - 48) * 60);
    tone.Q.value = 0.3;

    const gain = audioContext.createGain();
    const peak = 0.55 * velocity ** 1.4;
    let releaseStart = startTime + Math.max(duration, 0.03);

    // A key can only sound once: re-striking it damps the earlier note, as on a real piano.
    // Patterns may schedule a later strike first, so handle both orders.
    if (other && other.startTime > startTime) {
      releaseStart = Math.min(releaseStart, other.startTime);
    } else {
      if (other && other.releaseStart > startTime) {
        other.gain.gain.cancelScheduledValues(startTime);
        other.gain.gain.setTargetAtTime(0, startTime, 0.03);
      }
      soundingNotes.set(note, { gain, startTime, releaseStart });
    }

    gain.gain.setValueAtTime(0, startTime);
    gain.gain.linearRampToValueAtTime(peak, startTime + 0.004);
    gain.gain.setTargetAtTime(0, releaseStart, releaseStart > startTime + 0.01 ? DAMPER_SECONDS : 0.03);

    const panner = audioContext.createStereoPanner?.();
    let output = source.connect(tone).connect(gain);
    if (panner) {
      // Low notes slightly left, high notes slightly right, like sitting at the keyboard.
      panner.pan.value = Math.max(-0.35, Math.min(0.35, (note - 62) / 45));
      output = output.connect(panner);
    }
    output.connect(pianoBus);

    source.start(startTime);
    source.stop(releaseStart + DAMPER_SECONDS * 8);
    source.addEventListener("ended", () => {
      activeSources.delete(source);
      if (soundingNotes.get(note)?.gain === gain) soundingNotes.delete(note);
      (panner ?? gain).disconnect();
    }, { once: true });
    activeSources.set(source, gain);
  }

  // Chooses the inversion of the chord that moves least from the previous voicing
  // while staying around the middle of the keyboard.
  function voiceChord(chord, previous) {
    const tones = chord.intervals.map((interval) => pitchClass(chord.root + interval));
    let best;
    let bestCost = Infinity;
    tones.forEach((_, inversion) => {
      const order = [...tones.slice(inversion), ...tones.slice(0, inversion)];
      for (let low = VOICING_LOW; low <= VOICING_HIGH; low++) {
        if (pitchClass(low) !== order[0]) continue;
        const notes = [low];
        for (const tone of order.slice(1)) {
          let note = notes.at(-1) + 1;
          while (pitchClass(note) !== tone) note++;
          notes.push(note);
        }
        if (notes.at(-1) > VOICING_HIGH) continue;
        const centre = notes.reduce((sum, note) => sum + note, 0) / notes.length;
        let cost = Math.abs(centre - VOICING_CENTRE) * 0.5;
        if (previous) {
          cost += notes.reduce((sum, note) =>
            sum + Math.min(...previous.map((prev) => Math.abs(prev - note))), 0);
          cost += Math.abs(notes.at(-1) - previous.at(-1)) * 0.5; // keep the top line smooth
        }
        if (cost < bestCost) {
          bestCost = cost;
          best = notes;
        }
      }
    });
    return best;
  }

  function nearestNote(targetPitchClass, around) {
    const above = around + pitchClass(targetPitchClass - around);
    return above - around > 6 ? above - 12 : above;
  }

  function bassNoteFor(chord, previous) {
    const target = previous === undefined ? BASS_CENTRE : previous * 0.6 + BASS_CENTRE * 0.4;
    let note = nearestNote(chord.root, Math.round(target));
    while (note < BASS_LOW) note += 12;
    while (note > BASS_HIGH) note -= 12;
    return note;
  }

  // Accompaniment patterns. Positions and durations are in beats; bar.play adds swing,
  // humanised timing and velocity, and phrase-level dynamics.
  const PATTERNS = {
    // Pop ballad: pedalled left-hand octave, chord on 1, gentle broken figures after.
    ballad(bar) {
      const { voicing: v, bass, fifth } = bar;
      bar.play([bass, bass + 12], 0, HOLD, 0.5);
      bar.play([bass + fifth], 2, HOLD, 0.38);
      bar.play(v, 0, HOLD, 0.46, 0.014);
      bar.play([v.at(-1)], 1.5, HOLD, 0.27);
      bar.play(v.slice(1), 2, HOLD, 0.36, 0.01);
      bar.play([v[1]], 3, HOLD, 0.25);
      bar.play([v.at(-1)], 3.5, HOLD, 0.29);
    },

    // 50s doo-wop: repeated triplet chords over a stepping bass.
    triplet(bar) {
      const { voicing: v, bass, third, fifth } = bar;
      for (let i = 0; i < 12; i++) {
        bar.play(v, i / 3, bar.beats(0.28), i % 3 === 0 ? 0.34 : 0.21, 0.005);
      }
      [0, third, fifth, third].forEach((step, beat) =>
        bar.play([bass + step], beat, bar.beats(0.9), beat === 0 ? 0.5 : 0.4));
    },

    // Folk: pedalled bass with a flowing eighth-note arpeggio.
    arpeggio(bar) {
      const { voicing: v, bass, fifth } = bar;
      const tones = [v[0], v[1], v[2], v[0] + 12];
      bar.play([bass], 0, HOLD, 0.5);
      bar.play([bass + fifth], 2, HOLD, 0.36);
      [0, 1, 2, 3, 2, 1, 2].forEach((tone, i) =>
        bar.play([tones[tone]], (i + 1) / 2, HOLD, i === 3 ? 0.34 : 0.28));
    },

    // Rock: driving eighth-note chords over left-hand octaves.
    rock(bar) {
      const { voicing: v, bass } = bar;
      for (let i = 0; i < 8; i++) {
        const velocity = i % 4 === 0 ? 0.42 : i % 2 === 0 ? 0.36 : 0.25;
        bar.play(v, i / 2, bar.beats(0.42), velocity, 0.004);
      }
      for (let beat = 0; beat < 4; beat++) {
        bar.play([bass, bass + 12], beat, bar.beats(0.9), beat % 2 === 0 ? 0.46 : 0.38);
      }
    },

    // Jazz: walking bass with a chromatic approach to the next chord, and swung comping.
    swing(bar) {
      const { voicing: v, bass, third, fifth, seventh, chord, next } = bar;
      const nextRoot = bassNoteFor(next, bass); // the note the next bar will start on
      const approach = nextRoot + (Math.random() < 0.5 ? 1 : -1);
      const line = Math.random() < 0.5
        ? [bass, bass + third, bass + fifth, approach]
        : [bass, bass + fifth, bass + (seventh ?? 12), approach];
      line.forEach((note, beat) => bar.play([note], beat, bar.beats(0.92), beat === 0 ? 0.48 : 0.4));

      const comping = [
        [[0, 0.55, 0.34], [1.5, 1.2, 0.3]], // Charleston
        [[1.5, 0.5, 0.32], [3, 0.8, 0.3]],
        [[0, 0.5, 0.32], [2.5, 0.5, 0.3], [3.5, 0.45, 0.26]],
      ];
      // Leave out the root in the right hand when the bass already has it (shell voicing).
      const hand = v.length === 4 ? v.filter((note) => pitchClass(note) !== chord.root) : v;
      comping[Math.floor(Math.random() * comping.length)].forEach(([at, length, velocity]) =>
        bar.play(hand, at, bar.beats(length), velocity, 0.006));
    },

    // Blues: boogie-woogie left hand and swung stabs on 2 and 4.
    shuffle(bar) {
      const { voicing: v, bass, third } = bar;
      [0, third, 7, 9, 10, 9, 7, third].forEach((step, i) =>
        bar.play([bass + step], i / 2, bar.beats(0.45), i % 2 === 0 ? 0.44 : 0.33));
      bar.play(v, 1, bar.beats(0.35), 0.32, 0.004);
      bar.play(v, 3, bar.beats(0.35), 0.32, 0.004);
      if (Math.random() < 0.35) bar.play(v, 3.5, bar.beats(0.4), 0.25, 0.004);
    },
  };

  const SWING_STYLES = new Set(["swing", "shuffle"]);
  const PHRASE_DYNAMICS = [1, 0.93, 0.98, 0.9];

  function scheduleBar(index, barStart, beatDuration) {
    const chord = chords[index];
    const next = chords[(index + 1) % chords.length];
    const voicing = voiceChord(chord, previousVoicing);
    const bass = bassNoteFor(chord, previousBass);
    previousVoicing = voicing;
    previousBass = bass;

    const barEnd = barStart + 4 * beatDuration;
    // Swing softens at fast tempos, as players naturally do.
    const swingOffbeat = SWING_STYLES.has(style) ? (tempo > 160 ? 0.58 : 0.64) : 0.5;
    const dynamics = PHRASE_DYNAMICS[barsPlayed % PHRASE_DYNAMICS.length];
    barsPlayed++;

    const bar = {
      chord, next, voicing, bass,
      third: chord.intervals[1],
      fifth: chord.intervals[2],
      seventh: chord.intervals[3],
      beats: (count) => count * beatDuration,
      play(notes, at, duration, velocity, roll = 0) {
        const whole = Math.floor(at);
        const swung = at - whole === 0.5 ? whole + swingOffbeat : at;
        const start = Math.max(barStart, barStart + swung * beatDuration + humanize());
        notes.forEach((note, i) => {
          const noteStart = start + i * roll * random(0.7, 1.3);
          const length = duration === HOLD
            ? barEnd + PEDAL_OVERLAP_SECONDS - noteStart
            : duration - (noteStart - start);
          playNote(note, noteStart, length, Math.min(1, velocity * dynamics * random(0.92, 1.08)));
        });
      },
    };
    (PATTERNS[style] ?? PATTERNS.ballad)(bar);
  }

  function updateActiveBar(now) {
    while (scheduledBars.length > 1 && scheduledBars[1].start <= now) {
      scheduledBars.shift();
    }
    const current = scheduledBars[0];
    if (current && current.start <= now && current !== displayedBar) {
      displayedBar = current;
      setActiveChord(current.index);
      status.textContent =
        `Backing track: ${chords[current.index].name} (${current.index + 1} of ${chords.length})`;
    }
  }

  function tick() {
    try {
      const now = audioContext.currentTime;
      if (nextBarStart < now) {
        // The timer fell behind the audio clock (e.g. a heavily throttled tab). Resume from now
        // rather than scheduling a burst of notes in the past.
        nextBarStart = now + START_DELAY_SECONDS;
      }
      while (nextBarStart < now + SCHEDULE_AHEAD_SECONDS) {
        const barDuration = 240 / tempo;
        const index = nextBarIndex % chords.length;
        scheduleBar(index, nextBarStart, barDuration / 4);
        scheduledBars.push({ index, start: nextBarStart });
        nextBarIndex = index + 1;
        nextBarStart += barDuration;
      }
      updateActiveBar(now);
    } catch (error) {
      console.error("Piano chord progression playback failed.", error);
      stopPlayback("Playback failed. Please try again.");
    }
  }

  async function startPlayback() {
    if (!window.AudioContext) {
      status.textContent = "Audio playback is not supported in this browser.";
      return;
    }

    const requestedTempo = readTempo();
    if (requestedTempo === null) {
      status.textContent = `Enter a tempo between ${MIN_TEMPO} and ${MAX_TEMPO} BPM.`;
      tempoInput.focus();
      return;
    }

    const generation = ++playbackGeneration;
    try {
      ensureAudio();
      await audioContext.resume();
      if (generation !== playbackGeneration) return;
      playing = true;
      setPlayingUi(true);
      status.textContent = "Loading piano...";
      const loadedSamples = await loadPianoSamples();
      if (!playing || generation !== playbackGeneration) return;

      samples = loadedSamples;
      tempo = requestedTempo;
      nextBarIndex = 0;
      nextBarStart = audioContext.currentTime + START_DELAY_SECONDS;
      barsPlayed = 0;
      previousVoicing = undefined;
      previousBass = undefined;
      scheduledBars = [];
      displayedBar = undefined;
      status.textContent = "Starting progression...";
      tick();
      if (playing && generation === playbackGeneration) {
        schedulerTimer = window.setInterval(tick, SCHEDULER_INTERVAL_MS);
      }
    } catch (error) {
      if (generation === playbackGeneration) {
        stopPlayback("Unable to load piano audio. Please try again.");
      }
      console.error("Unable to play the piano chord progression.", error);
    }
  }

  toggleButton.addEventListener("click", () => {
    if (playing) {
      stopPlayback();
    } else {
      startPlayback();
    }
  });

  // Tempo changes apply live, from the next bar that has not been scheduled yet.
  tempoInput.addEventListener("input", () => {
    const value = readTempo();
    if (value !== null) tempo = value;
  });

  progressionType.addEventListener("change", () => {
    if (playing) stopPlayback("Progression changed. Press Play to start the new backing track.");
    syncControls();
  });

  scaleType.addEventListener("change", () => {
    if (playing) stopPlayback("Scale changed. Press Play to start the new backing track.");
    syncControls();
  });

  window.addEventListener("pagehide", () => {
    if (playing) stopPlayback();
  });

  window.addEventListener("pageshow", (event) => {
    if (event.persisted) syncControls();
  });

  syncControls();
})();
