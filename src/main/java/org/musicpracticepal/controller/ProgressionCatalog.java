package org.musicpracticepal.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.jackson.databind.json.JsonMapper;

/**
 * Practice keys and chord progressions, with chord names spelled correctly for each key.
 */
final class ProgressionCatalog {

    enum Quality {
        MAJOR(""), MINOR("m"), DOMINANT_SEVENTH("7"), MAJOR_SEVENTH("maj7"), MINOR_SEVENTH("m7"),
        DIMINISHED("dim"), DIMINISHED_SEVENTH("dim7"), HALF_DIMINISHED("m7b5");

        private final String suffix;

        Quality(String suffix) {
            this.suffix = suffix;
        }
    }

    record ChordDegree(int semitones, Quality quality) {}

    /** How the backing track plays a progression; mirrored by the patterns in chord-progression.js. */
    enum Style {
        BALLAD, TRIPLET, ARPEGGIO, ROCK, SWING, SHUFFLE;

        String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    record Progression(
            String id,
            Style style,
            String majorLabel,
            List<ChordDegree> majorDegrees,
            String minorLabel,
            List<ChordDegree> minorDegrees) {

        String label(String key) {
            return isMinor(key) ? minorLabel : majorLabel;
        }

        List<ChordDegree> degrees(String key) {
            return isMinor(key) ? minorDegrees : majorDegrees;
        }
    }

    private static final String MINOR_SUFFIX = " minor";

    static final List<String> MAJOR_KEYS = List.of(
            "C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B");
    static final List<String> MINOR_KEYS = List.of(
            "A minor", "Bb minor", "B minor", "C minor", "C# minor", "D minor",
            "D# minor", "E minor", "F minor", "F# minor", "G minor", "G# minor");
    static final List<String> KEYS = Stream.concat(MAJOR_KEYS.stream(), MINOR_KEYS.stream()).toList();

    private static final String LETTERS = "CDEFGAB";
    private static final int[] NATURAL_PITCHES = {0, 2, 4, 5, 7, 9, 11};
    /**
     * Scale step (0-based) for each semitone offset above the tonic; -1 means no diatonic step.
     * Six semitones is spelled as a raised fourth (the ♯IV°7 passing chord in the blues).
     */
    private static final int[] STEP_FOR_SEMITONES = {0, -1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6};

    static final List<Progression> PROGRESSIONS = List.of(
            new Progression("pop", Style.BALLAD,
                    "Pop (I–V–vi–IV)", List.of(major(0), major(7), minor(9), major(5)),
                    "Pop (i–v–VI–iv)", List.of(minor(0), minor(7), major(8), minor(5))),
            new Progression("doo-wop", Style.TRIPLET,
                    "50s / Doo-wop (I–vi–IV–V)", List.of(major(0), minor(9), major(5), major(7)),
                    "50s / Doo-wop (i–VI–iv–V)", List.of(minor(0), major(8), minor(5), major(7))),
            new Progression("minor-pop", Style.BALLAD,
                    "Pop / Rock (vi–IV–I–V)", List.of(minor(9), major(5), major(0), major(7)),
                    "Pop / Rock (i–VI–III–VII)", List.of(minor(0), major(8), major(3), major(10))),
            new Progression("folk-rock", Style.ARPEGGIO,
                    "Folk / Rock (I–IV–V–I)", List.of(major(0), major(5), major(7), major(0)),
                    "Folk / Rock (i–iv–V–i)", List.of(minor(0), minor(5), major(7), minor(0))),
            new Progression("classic-rock", Style.ROCK,
                    "Classic Rock (I–V–IV–I)", List.of(major(0), major(7), major(5), major(0)),
                    "Classic Rock (i–v–iv–i)", List.of(minor(0), minor(7), minor(5), minor(0))),
            new Progression("jazz-turnaround", Style.SWING,
                    "Jazz turnaround (ii7–V7–Imaj7–vi7)", List.of(
                            new ChordDegree(2, Quality.MINOR_SEVENTH),
                            new ChordDegree(7, Quality.DOMINANT_SEVENTH),
                            new ChordDegree(0, Quality.MAJOR_SEVENTH),
                            new ChordDegree(9, Quality.MINOR_SEVENTH)),
                    "Jazz turnaround (iiø7–V7–i7–VImaj7)", List.of(
                            new ChordDegree(2, Quality.HALF_DIMINISHED),
                            new ChordDegree(7, Quality.DOMINANT_SEVENTH),
                            new ChordDegree(0, Quality.MINOR_SEVENTH),
                            new ChordDegree(8, Quality.MAJOR_SEVENTH))),
            new Progression("blues", Style.SHUFFLE,
                    "12-bar Blues (I7–IV7–V7)", twelveBarBlues(Quality.DOMINANT_SEVENTH),
                    "12-bar minor Blues (i7–iv7–V7)", twelveBarBlues(Quality.MINOR_SEVENTH)),
            new Progression("quick-change-blues", Style.SHUFFLE,
                    "Quick-change Blues (I7–IV7–I7–V7)", List.of(
                            dom7(0), dom7(5), dom7(0), dom7(0), dom7(5), dom7(5),
                            dom7(0), dom7(0), dom7(7), dom7(5), dom7(0), dom7(7)),
                    "Quick-change minor Blues (i7–iv7–i7–V7)", List.of(
                            min7(0), min7(5), min7(0), min7(0), min7(5), min7(5),
                            min7(0), min7(0), dom7(7), min7(5), min7(0), dom7(7))),
            new Progression("slow-blues", Style.TRIPLET,
                    "Slow Blues 12/8 (I7–IV7–♯IV°7–V7)", List.of(
                            dom7(0), dom7(5), dom7(0), dom7(0), dom7(5), dim7(6),
                            dom7(0), dom7(0), dom7(7), dom7(5), dom7(0), dom7(7)),
                    "Slow minor Blues 12/8 (i7–iv7–♭VI7–V7)", List.of(
                            min7(0), min7(0), min7(0), min7(0), min7(5), min7(5),
                            min7(0), min7(0), dom7(8), dom7(7), min7(0), min7(0))),
            new Progression("eight-bar-blues", Style.SHUFFLE,
                    "8-bar Blues (I7–V7–IV7)", List.of(
                            dom7(0), dom7(7), dom7(5), dom7(5), dom7(0), dom7(7), dom7(0), dom7(7)),
                    "8-bar minor Blues (i7–V7–iv7)", List.of(
                            min7(0), dom7(7), min7(5), min7(5), min7(0), dom7(7), min7(0), dom7(7))),
            new Progression("jazz-blues", Style.SWING,
                    "Jazz Blues (I7–IV7–♯IV°7–VI7–ii7–V7)", List.of(
                            dom7(0), dom7(5), dom7(0), dom7(0), dom7(5), dim7(6),
                            dom7(0), dom7(9), min7(2), dom7(7), dom7(0), dom7(7)),
                    "Jazz minor Blues (i7–iv7–♭VI7–V7)", List.of(
                            min7(0), min7(5), min7(0), min7(0), min7(5), min7(5),
                            min7(0), min7(0), dom7(8), dom7(7), min7(0), dom7(7))));

    /** JSON consumed by chord-progression.js: progression id -> labels and chord sequence per key. */
    static final String PROGRESSION_DATA_JSON = buildProgressionDataJson();

    private ProgressionCatalog() {}

    static boolean isMinor(String key) {
        return key.endsWith(MINOR_SUFFIX);
    }

    static Progression progression(String id) {
        return PROGRESSIONS.stream()
                .filter(progression -> progression.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported progression: " + id));
    }

    static List<String> chords(Progression progression, String key) {
        requireKey(key);
        String tonic = tonic(key);
        return progression.degrees(key).stream()
                .map(degree -> spell(tonic, degree.semitones()) + degree.quality().suffix)
                .toList();
    }

    static String chordSequence(Progression progression, String key) {
        return String.join("|", chords(progression, key));
    }

    /** "C (A minor)" for major keys, "A minor (C major)" for minor keys. */
    static String displayName(String key) {
        requireKey(key);
        return isMinor(key)
                ? key + " (" + spell(tonic(key), 3) + " major)"
                : key + " (" + spell(key, 9) + MINOR_SUFFIX + ")";
    }

    /** Spells the note {@code semitones} above {@code tonic} using the letter of the matching scale step. */
    static String spell(String tonic, int semitones) {
        int tonicLetter = LETTERS.indexOf(tonic.charAt(0));
        if (tonicLetter < 0) {
            throw new IllegalArgumentException("Unsupported tonic: " + tonic);
        }
        int tonicPitch = Math.floorMod(NATURAL_PITCHES[tonicLetter] + accidentalValue(tonic.substring(1)), 12);
        int interval = Math.floorMod(semitones, 12);
        int step = STEP_FOR_SEMITONES[interval];
        if (step < 0) {
            throw new IllegalArgumentException("No diatonic spelling for " + semitones + " semitones");
        }
        int letter = (tonicLetter + step) % LETTERS.length();
        int pitch = (tonicPitch + interval) % 12;
        String accidental = switch (Math.floorMod(pitch - NATURAL_PITCHES[letter], 12)) {
            case 0 -> "";
            case 1 -> "#";
            case 2 -> "##";
            case 11 -> "b";
            case 10 -> "bb";
            default -> throw new IllegalStateException("Cannot spell " + semitones + " semitones above " + tonic);
        };
        return LETTERS.charAt(letter) + accidental;
    }

    private static int accidentalValue(String accidental) {
        return switch (accidental) {
            case "" -> 0;
            case "#" -> 1;
            case "##" -> 2;
            case "b" -> -1;
            case "bb" -> -2;
            default -> throw new IllegalArgumentException("Unsupported accidental: " + accidental);
        };
    }

    private static String tonic(String key) {
        return isMinor(key) ? key.substring(0, key.length() - MINOR_SUFFIX.length()) : key;
    }

    private static void requireKey(String key) {
        if (!KEYS.contains(key)) {
            throw new IllegalArgumentException("Unsupported key: " + key);
        }
    }

    private static ChordDegree major(int semitones) {
        return new ChordDegree(semitones, Quality.MAJOR);
    }

    private static ChordDegree minor(int semitones) {
        return new ChordDegree(semitones, Quality.MINOR);
    }

    private static ChordDegree dom7(int semitones) {
        return new ChordDegree(semitones, Quality.DOMINANT_SEVENTH);
    }

    private static ChordDegree min7(int semitones) {
        return new ChordDegree(semitones, Quality.MINOR_SEVENTH);
    }

    private static ChordDegree dim7(int semitones) {
        return new ChordDegree(semitones, Quality.DIMINISHED_SEVENTH);
    }

    private static List<ChordDegree> twelveBarBlues(Quality tonicAndSubdominant) {
        ChordDegree one = new ChordDegree(0, tonicAndSubdominant);
        ChordDegree four = new ChordDegree(5, tonicAndSubdominant);
        ChordDegree five = new ChordDegree(7, Quality.DOMINANT_SEVENTH);
        return List.of(one, one, one, one, four, four, one, one, five, four, one, one);
    }

    private static String buildProgressionDataJson() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Progression progression : PROGRESSIONS) {
            Map<String, String> chordsByKey = KEYS.stream().collect(Collectors.toMap(
                    key -> key, key -> chordSequence(progression, key),
                    (first, second) -> first, LinkedHashMap::new));
            data.put(progression.id(), Map.of(
                    "style", progression.style().id(),
                    "majorLabel", progression.majorLabel(),
                    "minorLabel", progression.minorLabel(),
                    "chords", chordsByKey));
        }
        return JsonMapper.builder().build().writeValueAsString(data);
    }
}
