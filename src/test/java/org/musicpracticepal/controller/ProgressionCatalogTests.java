package org.musicpracticepal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ProgressionCatalogTests {

    private static final Pattern CHORD = Pattern.compile("([A-G])(#|b)?(maj7|m7b5|m7|m|7|dim7|dim)?");
    private static final Map<Character, Integer> LETTER_PITCHES =
            Map.of('C', 0, 'D', 2, 'E', 4, 'F', 5, 'G', 7, 'A', 9, 'B', 11);

    private static String chords(String progressionId, String key) {
        return ProgressionCatalog.chordSequence(ProgressionCatalog.progression(progressionId), key);
    }

    @Test
    void spellsChordsForTheKey() {
        assertEquals("C|G|Am|F", chords("pop", "C"));
        assertEquals("B|F#|G#m|E", chords("pop", "B"));
        assertEquals("Db|Ab|Bbm|Gb", chords("pop", "Db"));
        assertEquals("F#|C#|D#m|B", chords("pop", "F#"));
        assertEquals("A|F#m|D|E", chords("doo-wop", "A"));
        assertEquals("Am7|D7|Gmaj7|Em7", chords("jazz-turnaround", "G"));
        assertEquals("A7|A7|A7|A7|D7|D7|A7|A7|E7|D7|A7|A7", chords("blues", "A"));
    }

    @Test
    void spellsBluesForms() {
        assertEquals("A7|D7|A7|A7|D7|D7|A7|A7|E7|D7|A7|E7", chords("quick-change-blues", "A"));
        assertEquals("C7|F7|C7|C7|F7|F#dim7|C7|C7|G7|F7|C7|G7", chords("slow-blues", "C"));
        assertEquals("Db7|Gb7|Db7|Db7|Gb7|Gdim7|Db7|Db7|Ab7|Gb7|Db7|Ab7", chords("slow-blues", "Db"));
        assertEquals("E7|B7|A7|A7|E7|B7|E7|B7", chords("eight-bar-blues", "E"));
        assertEquals("Bb7|Eb7|Bb7|Bb7|Eb7|Edim7|Bb7|G7|Cm7|F7|Bb7|F7", chords("jazz-blues", "Bb"));
        assertEquals("Am7|Am7|Am7|Am7|Dm7|Dm7|Am7|Am7|F7|E7|Am7|Am7", chords("slow-blues", "A minor"));
        assertEquals("Cm7|Fm7|Cm7|Cm7|Fm7|Fm7|Cm7|Cm7|Ab7|G7|Cm7|G7", chords("jazz-blues", "C minor"));
        assertEquals("Em7|B7|Am7|Am7|Em7|B7|Em7|B7", chords("eight-bar-blues", "E minor"));
    }

    @Test
    void minorKeysUseMinorProgressions() {
        assertEquals("Am|Em|F|Dm", chords("pop", "A minor"));
        assertEquals("Am|F|C|G", chords("minor-pop", "A minor"));
        assertEquals("Am|F|Dm|E", chords("doo-wop", "A minor"));
        assertEquals("Am|Dm|E|Am", chords("folk-rock", "A minor"));
        assertEquals("Am|Em|Dm|Am", chords("classic-rock", "A minor"));
        assertEquals("Bm7b5|E7|Am7|Fmaj7", chords("jazz-turnaround", "A minor"));
        assertEquals("E#m7b5|A#7|D#m7|Bmaj7", chords("jazz-turnaround", "D# minor"));
        assertEquals("Am7|Am7|Am7|Am7|Dm7|Dm7|Am7|Am7|E7|Dm7|Am7|Am7", chords("blues", "A minor"));
        assertEquals("Jazz turnaround (iiø7–V7–i7–VImaj7)",
                ProgressionCatalog.progression("jazz-turnaround").label("A minor"));
    }

    @Test
    void everyChordMatchesItsPitchAndUsesAtMostOneAccidental() {
        for (ProgressionCatalog.Progression progression : ProgressionCatalog.PROGRESSIONS) {
            for (String key : ProgressionCatalog.KEYS) {
                List<String> chords = ProgressionCatalog.chords(progression, key);
                List<ProgressionCatalog.ChordDegree> degrees = progression.degrees(key);
                String tonic = key.replace(" minor", "");
                for (int i = 0; i < chords.size(); i++) {
                    Matcher matcher = CHORD.matcher(chords.get(i));
                    assertTrue(matcher.matches(), "Unexpected chord " + chords.get(i) + " in " + key);
                    int expected = Math.floorMod(pitchOf(tonic) + degrees.get(i).semitones(), 12);
                    assertEquals(expected, pitchOf(matcher.group(1) + nullToEmpty(matcher.group(2))),
                            chords.get(i) + " in " + key);
                }
            }
        }
    }

    @Test
    void progressionsAreDistinctInEveryKey() {
        for (String key : ProgressionCatalog.KEYS) {
            Set<String> sequences = ProgressionCatalog.PROGRESSIONS.stream()
                    .map(progression -> ProgressionCatalog.chordSequence(progression, key))
                    .collect(Collectors.toSet());
            assertEquals(ProgressionCatalog.PROGRESSIONS.size(), sequences.size(), key);
        }
    }

    @Test
    void keysCoverEachPitchOnceWithRelativeNames() {
        Set<Integer> majorPitches = new HashSet<>();
        ProgressionCatalog.MAJOR_KEYS.forEach(key -> majorPitches.add(pitchOf(key)));
        assertEquals(12, majorPitches.size());
        Set<Integer> minorPitches = new HashSet<>();
        ProgressionCatalog.MINOR_KEYS.forEach(key -> minorPitches.add(pitchOf(key.replace(" minor", ""))));
        assertEquals(12, minorPitches.size());

        assertEquals("C (A minor)", ProgressionCatalog.displayName("C"));
        assertEquals("F# (D# minor)", ProgressionCatalog.displayName("F#"));
        assertEquals("Db (Bb minor)", ProgressionCatalog.displayName("Db"));
        assertEquals("Bb minor (Db major)", ProgressionCatalog.displayName("Bb minor"));
        assertEquals("G# minor (B major)", ProgressionCatalog.displayName("G# minor"));
        for (String key : ProgressionCatalog.KEYS) {
            String display = ProgressionCatalog.displayName(key);
            String relative = display.substring(display.indexOf('(') + 1, display.length() - 1)
                    .replace(" major", "");
            assertTrue(ProgressionCatalog.KEYS.contains(relative), "Relative key not offered: " + display);
        }
    }

    @Test
    void progressionDataJsonContainsLabelsAndChordsForEveryKey() {
        String json = ProgressionCatalog.PROGRESSION_DATA_JSON;
        assertTrue(json.contains("\"majorLabel\":\"Pop (I–V–vi–IV)\""));
        assertTrue(json.contains("\"minorLabel\":\"Pop / Rock (i–VI–III–VII)\""));
        assertTrue(json.contains("\"B\":\"B|F#|G#m|E\""));
        assertTrue(json.contains("\"A minor\":\"Bm7b5|E7|Am7|Fmaj7\""));
        assertTrue(json.contains("\"style\":\"swing\""));
        assertTrue(json.contains("\"style\":\"triplet\""));
        assertFalse(json.contains("<"));
    }

    private static int pitchOf(String note) {
        int pitch = LETTER_PITCHES.get(note.charAt(0));
        for (char accidental : note.substring(1).toCharArray()) {
            pitch += accidental == '#' ? 1 : -1;
        }
        return Math.floorMod(pitch, 12);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
