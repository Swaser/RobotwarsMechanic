package com.noser.robotwars.mechanic

import org.junit.Test
import kotlin.test.assertEquals

class CouplerTest {

    @Volatile
    var a: Int = -10

    @Test
    fun veryNoStackOverflow() {

        a = -10
        conduct(10_000)

        Thread.sleep(3_000)

        assertEquals(0, a)
    }

    private fun conduct(i: Int) {

        if (i >= 0)
            ExcutorDecoupler
                .some(Unit)
                .later { a = i }
                .later { conduct(i - 1) }
    }

}

