package com.camerondavison.webkit2png.webkit;

import org.junit.Test;

/**
 * User: loyd
 * Date: 7/29/11
 */
public class WebkitRendererTest {
    @Test(expected = RuntimeException.class)
    public void testInstance() throws Exception {
        new WebkitRenderer();
    }
}
