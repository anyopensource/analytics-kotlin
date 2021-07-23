package com.segment.analytics.kotlin.core

import com.segment.analytics.kotlin.core.utils.StubPlugin
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection

class SettingsTests {

    private lateinit var analytics: Analytics
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    init {
        mockkConstructor(HTTPClient::class)
        val settingsStream = ByteArrayInputStream(
            """
                {"integrations":{"Segment.io":{"apiKey":"1vNgUqwJeCHmqgI9S1sOm9UHCyfYqbaQ"}},"plan":{},"edgeFunction":{}}
            """.trimIndent().toByteArray()
        )
        val httpConnection: HttpURLConnection = mockk()
        val connection = object : Connection(httpConnection, settingsStream, null) {}
        every { anyConstructed<HTTPClient>().settings(any()) } returns connection
    }

    @BeforeEach
    fun setup() {
        analytics = Analytics(
            Configuration(
                writeKey = "123",
                analyticsScope = testScope,
                ioDispatcher = testDispatcher,
                analyticsDispatcher = testDispatcher,
                application = "Test"
            )
        )
        analytics.configuration.autoAddSegmentDestination = false
    }

    @Test
    fun `checkSettings updates settings`() = runBlocking {
        val system = analytics.store.currentState(System::class)
        val curSettings = system?.settings
        Assertions.assertEquals(
            Settings(
                integrations = buildJsonObject {
                    put(
                        "Segment.io",
                        buildJsonObject { put("apiKey", "1vNgUqwJeCHmqgI9S1sOm9UHCyfYqbaQ") })
                },
                plan = emptyJsonObject,
                edgeFunction = emptyJsonObject
            ),
            curSettings
        )
    }

    @Test
    fun `settings update updates plugins`() = runBlocking {
        val mockPlugin = spyk(StubPlugin())
        analytics.add(mockPlugin)
        verify {
            mockPlugin.update(
                Settings(
                    integrations = buildJsonObject {
                        put(
                            "Segment.io",
                            buildJsonObject {
                                put(
                                    "apiKey",
                                    "1vNgUqwJeCHmqgI9S1sOm9UHCyfYqbaQ"
                                )
                            })
                    },
                    plan = emptyJsonObject,
                    edgeFunction = emptyJsonObject
                )
            )
        }
    }
}