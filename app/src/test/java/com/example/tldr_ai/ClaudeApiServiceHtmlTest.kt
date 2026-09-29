package com.example.tldr_ai

import com.example.tldr_ai.data.api.ClaudeApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeApiServiceHtmlTest {

    @Test
    fun preprocessHtml_stripsNonContentBlocksAndKeepsArticleText() {
        val html = """
            <html>
              <head><script>trackPageView();</script><style>.hero { color: red; }</style></head>
              <body>
                <header>Site Header Nav Links</header>
                <nav>Home About Contact</nav>
                <article><p>The quick brown fox jumps over the lazy dog.</p></article>
                <footer>Copyright 2026 Example Corp</footer>
              </body>
            </html>
        """.trimIndent()

        val result = ClaudeApiService().preprocessHtml(html)

        assertTrue(result.contains("The quick brown fox jumps over the lazy dog."))
        assertFalse(result.contains("trackPageView"))
        assertFalse(result.contains("color: red"))
        assertFalse(result.contains("Site Header Nav Links"))
        assertFalse(result.contains("Home About Contact"))
        assertFalse(result.contains("Copyright 2026 Example Corp"))
    }

    @Test
    fun preprocessHtml_removesHtmlCommentsIncludingTagLikeContent() {
        val html = "<p>Before</p><!-- <div>hidden tag-like content</div> --><p>After</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("hidden"))
        assertFalse(result.contains("<div>"))
        assertTrue(result.contains("Before"))
        assertTrue(result.contains("After"))
    }

    @Test
    fun preprocessHtml_doesNotGlueWordsFromAdjacentBlockTags() {
        val html = "<p>Hello</p><p>World</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertTrue(result.contains("Hello World"))
        assertFalse(result.contains("HelloWorld"))
    }

    @Test
    fun preprocessHtml_removesIframeBlockAndContents() {
        val html = "<p>Keep</p><iframe src=\"https://ads.example.com\">fallback ad text</iframe><p>Also keep</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("fallback ad text"))
        assertFalse(result.contains("iframe"))
        assertTrue(result.contains("Keep"))
        assertTrue(result.contains("Also keep"))
    }

    @Test
    fun preprocessHtml_removesFormBlockAndContents() {
        val html = "<p>Keep</p><form action=\"/subscribe\"><input type=\"email\"><button>Subscribe</button></form>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("Subscribe"))
        assertTrue(result.contains("Keep"))
    }

    @Test
    fun preprocessHtml_removesSvgBlockAndContents() {
        val html = "<p>Keep</p><svg viewBox=\"0 0 10 10\"><title>icon label</title><path d=\"M0 0\"/></svg>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("icon label"))
        assertTrue(result.contains("Keep"))
    }

    @Test
    fun preprocessHtml_removesNoscriptBlockAndContents() {
        val html = "<p>Keep</p><noscript>Please enable JavaScript to view this content</noscript>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("enable JavaScript"))
        assertTrue(result.contains("Keep"))
    }

    @Test
    fun preprocessHtml_decodesNamedAndNumericEntities() {
        val html = "<p>Tom &amp; Jerry &lt;3 &gt; this &quot;test&quot; &#39;quote&#39; " +
            "&apos;apos&apos; a&nbsp;b &#65;</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertEquals(
            "Tom & Jerry <3 > this \"test\" 'quote' 'apos' a b A",
            result
        )
    }

    @Test
    fun preprocessHtml_collapsesWhitespaceAndTrimsResult() {
        val html = "  <p>  Line one  \n\n\t  Line   two  </p>  \n  "

        val result = ClaudeApiService().preprocessHtml(html)

        assertEquals("Line one Line two", result)
    }

    @Test
    fun preprocessHtml_isCaseInsensitiveForRemovedTags() {
        val html = "<SCRIPT>doStuff();</SCRIPT><Style>.a{}</Style><NAV>Nav Text</NAV>" +
            "<Header>Head Text</Header><FOOTER>Foot Text</FOOTER><p>Body Text</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertEquals("Body Text", result)
    }

    @Test
    fun preprocessHtml_removesMultipleScriptBlocks() {
        val html = "<script>first();</script><p>Middle</p><script>second();</script><p>End</p>"

        val result = ClaudeApiService().preprocessHtml(html)

        assertFalse(result.contains("first();"))
        assertFalse(result.contains("second();"))
        assertEquals("Middle End", result)
    }
}
