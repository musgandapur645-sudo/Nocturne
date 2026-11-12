package com.never.nocturne.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataTest {
    @Test
    fun removesControlCharactersAndTrimsMetadata() {
        assertEquals("Nocturne", cleanMetadataValue(" \u0000Nocturne\u0000 "))
    }

    @Test
    fun treatsBlankMetadataAsMissing() {
        assertEquals(null, cleanMetadataValue(" \u0000\t "))
        assertEquals(null, cleanMetadataValue(null))
    }
}
