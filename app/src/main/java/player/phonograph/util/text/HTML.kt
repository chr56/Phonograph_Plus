/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package player.phonograph.util.text

import org.intellij.lang.annotations.Language
import androidx.annotation.ColorInt
import android.graphics.Color


@Language("CSS")
fun changelogCSS(
    @ColorInt content_background_color: Int,
    @ColorInt text_color: Int,
    @ColorInt highlight_color: Int,
    @ColorInt disable_color: Int,
) = """
        * {
            word-wrap: break-word;
        }
        body {
            background-color: ${colorToCSS(content_background_color)};
            color: ${colorToCSS(text_color)};
        }
        a {
            color: ${colorToCSS(highlight_color)};
        }
        a:active {
            color: ${colorToCSS(highlight_color)};
        }
        h3 {
            margin-top: 1ex;
            margin-bottom: 1ex;
        }
        h4,
        h5 {
            padding: 0;
            margin: 0;
            margin-top: 2ex;
            margin-bottom: 0.5ex;
        }
        ol,
        ul {
            list-style-position: inside;
            border: 0;
            padding: 0;
            margin: 0;
            margin-left: 0.5ex;
        }
        li {
            padding: 1px;
            margin: 0;
            margin-left: 1ex;
        }
        p {
            margin: 0.75ex;
        }
        .highlight-text{
            color: $highlight_color;
        }
        .fine-print{
            color: $disable_color;
            font-size: small;
        }"""

@Language("HTML")
fun changelogHTML(
    CSS: String,
    content: String,
): String =
    """
        <html>
        <head>
        <style type="text/css">
        $CSS
        </style>
        </head>
        <body>
        $content
        </body>
        </html>
        """


// on API 29, WebView doesn't load with hex colors
private fun colorToCSS(color: Int): String =
    String.format(
        "rgb(%d, %d, %d)", Color.red(color), Color.green(color), Color.blue(color)
    )