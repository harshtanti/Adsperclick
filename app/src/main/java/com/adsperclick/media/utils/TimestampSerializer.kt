package com.adsperclick.media.utils

import com.google.firebase.Timestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TimestampSerializer : KSerializer<Timestamp> {
    override val descriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeLong(value.seconds * 1000 + value.nanoseconds / 1_000_000) // Convert to milliseconds
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        val millis = decoder.decodeLong()
        return Timestamp(millis / 1000, (millis % 1000).toInt() * 1_000_000) // Convert back to Timestamp
    }
}