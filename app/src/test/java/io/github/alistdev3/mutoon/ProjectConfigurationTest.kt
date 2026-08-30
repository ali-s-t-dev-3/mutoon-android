package io.github.alistdev3.mutoon

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun packageNameRemainsStable() {
        assertEquals("io.github.alistdev3.mutoon", javaClass.packageName)
    }
}
