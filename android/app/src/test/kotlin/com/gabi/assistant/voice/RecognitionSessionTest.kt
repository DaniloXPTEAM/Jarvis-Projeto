package com.gabi.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class RecognitionSessionTest {
    @Test fun refusesAnotherStartWhileWaitingForFinalResult() {
        val state = RecognitionSession()
        val id = state.start(0)!!
        state.ready(id, 1)
        state.ended(id, 100)
        assertNull(state.start(200))
        assertTrue(state.finish(id))
        assertNotNull(state.start(300))
    }
    @Test fun cancelledSessionRejectsLateResultsAndCannotFinishNewSession() {
        val state = RecognitionSession()
        val old = state.start(0)!!
        state.cancel()
        val fresh = state.start(1)!!
        assertFalse(state.finish(old))
        assertTrue(state.accepts(fresh))
    }
    @Test fun noReadyCallbackTimesOut() {
        val state = RecognitionSession()
        val id = state.start(100)!!
        assertFalse(state.timedOut(id, 10_099))
        assertTrue(state.timedOut(id, 10_100))
    }
    @Test fun repeatedPartialDoesNotKeepDeadRecognizerAlive() {
        val state = RecognitionSession()
        val id = state.start(0)!!
        state.ready(id, 1)
        state.partial(id, "Gabi", 100)
        state.partial(id, "Gabi", 10_000)
        assertTrue(state.timedOut(id, 15_100))
    }
    @Test fun latePartialsAndRepeatedEndDoNotExtendFinalResultDeadline() {
        val state = RecognitionSession()
        val id = state.start(0)!!
        state.ended(id, 100)
        state.partial(id, "Gabi ajuda", 7_000)
        state.ended(id, 7_500)
        assertTrue(state.timedOut(id, 8_100))
    }
    @Test fun changingPartialsStillHaveAbsoluteDeadline() {
        val state = RecognitionSession()
        val id = state.start(0)!!
        for (i in 1..89) state.partial(id, "palavra $i", i * 1000L)
        assertTrue(state.timedOut(id, 90_000))
    }
    @Test fun completedSessionCannotEmitAnotherTerminalEvent() {
        val state = RecognitionSession()
        val id = state.start(0)!!
        assertTrue(state.finish(id))
        assertFalse(state.finish(id))
        assertFalse(state.timedOut(id, 100_000))
    }
}
