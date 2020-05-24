/*
 * Licensed to the Light Team Software (Light Team) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Light Team licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightteam.editorkit.internal

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import androidx.core.text.getSpans
import com.lightteam.editorkit.R
import com.lightteam.editorkit.converter.ColorSchemeConverter
import com.lightteam.editorkit.feature.findreplace.FindResultSpan
import com.lightteam.language.language.Language
import com.lightteam.language.parser.span.ErrorSpan
import com.lightteam.language.scheme.SyntaxScheme
import com.lightteam.language.styler.Styleable
import com.lightteam.language.styler.span.StyleSpan
import com.lightteam.language.styler.span.SyntaxHighlightSpan
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

open class SyntaxHighlightEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : UndoRedoEditText(context, attrs, defStyleAttr), Styleable {

    var isSyntaxHighlighting = false
    var isErrorSpansVisible = false

    var isRegexEnabled = false
    var isMatchCaseEnabled = true

    var language: Language? = null

    private val syntaxHighlightSpans = mutableListOf<SyntaxHighlightSpan>()
    private val findResultSpans = mutableListOf<FindResultSpan>()
    private val delimiters = charArrayOf('{', '[', '(', '}', ']', ')')

    private var syntaxScheme: SyntaxScheme? = null
    private var findResultStyleSpan: StyleSpan? = null
    private var openDelimiterSpan: BackgroundColorSpan? = null
    private var closedDelimiterSpan: BackgroundColorSpan? = null

    private var topDirtyLine = 0
    private var bottomDirtyLine = 0

    private var addedTextCount = 0
    private var selectedFindResult = 0

    override fun colorize() {
        colorScheme?.let {
            syntaxScheme = ColorSchemeConverter.toSyntaxScheme(it)
            findResultStyleSpan = StyleSpan(color = it.findResultBackgroundColor)
            openDelimiterSpan = BackgroundColorSpan(it.delimiterBackgroundColor)
            closedDelimiterSpan = BackgroundColorSpan(it.delimiterBackgroundColor)
        }
        super.colorize()
    }

    override fun processText(newText: String) {
        syntaxHighlightSpans.clear()
        findResultSpans.clear()
        super.processText(newText)
        syntaxHighlight()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart == selEnd) {
            checkMatchingBracket(selStart)
        }
        // invalidate()
    }

    override fun setSelection(start: Int, stop: Int) {
        if (start <= text.length && stop <= text.length) {
            super.setSelection(start, stop)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateSyntaxHighlighting()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert)
        updateSyntaxHighlighting()
    }

    override fun doBeforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
        addedTextCount -= count
        cancelSyntaxHighlighting()
        if (!isSyntaxHighlighting) {
            super.doBeforeTextChanged(text, start, count, after)
        }
        abortFling()
    }

    override fun doOnTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        addedTextCount += count
        if (!isSyntaxHighlighting) {
            super.doOnTextChanged(text, start, before, count)
        }
    }

    override fun doAfterTextChanged(text: Editable?) {
        super.doAfterTextChanged(text)
        if (!isSyntaxHighlighting) {
            shiftSpans(selectionStart, addedTextCount)
        }
        addedTextCount = 0
        syntaxHighlight()
    }

    override fun setSpans(spans: List<SyntaxHighlightSpan>) {
        syntaxHighlightSpans.clear()
        syntaxHighlightSpans.addAll(spans)
        updateSyntaxHighlighting()
    }

    fun clearFindResultSpans() {
        selectedFindResult = 0
        findResultSpans.clear()
        val spans = text.getSpans<FindResultSpan>(0, text.length)
        for (span in spans) {
            text.removeSpan(span)
        }
    }

    fun setErrorLine(lineNumber: Int) {
        if (lineNumber == 0) {
            return
        }
        val lineStart = getIndexForStartOfLine(lineNumber - 1)
        val lineEnd = getIndexForEndOfLine(lineNumber - 1)
        if (lineStart < text.length && lineEnd < text.length && lineStart > -1 && lineEnd > -1) {
            isErrorSpansVisible = true
            text.setSpan(ErrorSpan(), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun find(findText: String) {
        if (findText.isNotEmpty()) {
            try {
                val pattern = if (isRegexEnabled) {
                    if (isMatchCaseEnabled) {
                        Pattern.compile(findText)
                    } else {
                        Pattern.compile(
                            findText,
                            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
                        )
                    }
                } else {
                    if (isMatchCaseEnabled) {
                        Pattern.compile(Pattern.quote(findText))
                    } else {
                        Pattern.compile(
                            Pattern.quote(findText),
                            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
                        )
                    }
                }
                val matcher = pattern.matcher(text)
                while (matcher.find()) {
                    findResultStyleSpan?.let {
                        val findResultSpan = FindResultSpan(it, matcher.start(), matcher.end())
                        findResultSpans.add(findResultSpan)

                        text.setSpan(
                            findResultSpan,
                            findResultSpan.start,
                            findResultSpan.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                if (findResultSpans.isNotEmpty()) {
                    selectResult()
                }
            } catch (e: PatternSyntaxException) {
                // nothing
            }
        }
    }

    fun findNext() {
        if (selectedFindResult < findResultSpans.size - 1) {
            selectedFindResult += 1
            selectResult()
        }
    }

    fun findPrevious() {
        if (selectedFindResult > 0 && selectedFindResult < findResultSpans.size) {
            selectedFindResult -= 1
            selectResult()
        }
    }

    fun replaceFindResult(replaceText: String) {
        if (findResultSpans.isNotEmpty()) {
            val findResult = findResultSpans[selectedFindResult]
            text.replace(findResult.start, findResult.end, replaceText)
            findResultSpans.remove(findResult)
            if (selectedFindResult >= findResultSpans.size) {
                selectedFindResult--
            }
        }
    }

    fun replaceAllFindResults(replaceText: String) {
        if (findResultSpans.isNotEmpty()) {
            val stringBuilder = StringBuilder(text)
            for (index in findResultSpans.size - 1 downTo 0) {
                val findResultSpan = findResultSpans[index]
                stringBuilder.replace(findResultSpan.start, findResultSpan.end, replaceText)
                findResultSpans.removeAt(index)
            }
            setText(stringBuilder.toString())
        }
    }

    private fun selectResult() {
        val findResult = findResultSpans[selectedFindResult]
        setSelection(findResult.start, findResult.end)
        scrollToFindResult()
    }

    private fun scrollToFindResult() {
        if (selectedFindResult < findResultSpans.size) {
            val findResult = findResultSpans[selectedFindResult]
            val topVisibleLine = getTopVisibleLine()
            val bottomVisibleLine = getBottomVisibleLine()
            if (findResult.start >= layout.getLineStart(topVisibleLine)) {
                if (findResult.end <= layout.getLineEnd(bottomVisibleLine)) {
                    return
                }
            }
            val height = layout.height - height + paddingBottom + paddingTop
            var lineTop = layout.getLineTop(layout.getLineForOffset(findResult.start))
            if (lineTop > height) {
                lineTop = height
            }
            val scrollX = if (!config.wordWrap) {
                layout.getPrimaryHorizontal(findResult.start).toInt()
            } else {
                scrollX
            }
            scrollTo(scrollX, lineTop)
        }
    }

    private fun shiftSpans(from: Int, byHowMuch: Int) {
        for (span in syntaxHighlightSpans) {
            if (span.start >= from) {
                span.start += byHowMuch
            }
            if (span.end >= from) {
                span.end += byHowMuch
            }
            /*if (span.start > span.end) {
                syntaxHighlightSpans.remove(span) // FIXME may cause ConcurrentModificationException
            }*/
        }
        for (findResult in findResultSpans) {
            /*if (from > findResult.start && from <= findResult.end) {
                findResultSpans.remove(findResult) // FIXME may cause IndexOutOfBoundsException
            }*/
            if (findResult.start > from) {
                findResult.start += byHowMuch
            }
            if (findResult.end >= from) {
                findResult.end += byHowMuch
            }
        }
        if (isErrorSpansVisible) {
            val spans = text.getSpans<ErrorSpan>(0, text.length)
            for (span in spans) {
                text.removeSpan(span)
            }
            isErrorSpansVisible = false
        }
    }

    private fun updateSyntaxHighlighting() {
        if (layout != null) {
            var topLine = scrollY / lineHeight - 10
            if (topLine >= lineCount) {
                topLine = lineCount - 1
            } else if (topLine < 0) {
                topLine = 0
            }

            var bottomLine = (scrollY + height) / lineHeight + 10
            if (bottomLine >= lineCount) {
                bottomLine = lineCount - 1
            } else if (bottomLine < 0) {
                bottomLine = 0
            }

            topDirtyLine = topLine
            bottomDirtyLine = bottomLine

            val lineStart = layout.getLineStart(topLine)
            val lineEnd = layout.getLineEnd(bottomLine)

            isSyntaxHighlighting = true
            val textSyntaxSpans = text.getSpans<SyntaxHighlightSpan>(0, text.length)
            for (span in textSyntaxSpans) {
                text.removeSpan(span)
            }
            for (span in syntaxHighlightSpans) {
                if (span.start >= 0 && span.end <= text.length && span.start <= span.end
                    && (span.start in lineStart..lineEnd || span.start <= lineEnd && span.end >= lineStart)) {
                    text.setSpan(
                        span,
                        if (span.start < lineStart) lineStart else span.start,
                        if (span.end > lineEnd) lineEnd else span.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            isSyntaxHighlighting = false

            val textFindSpans = text.getSpans<FindResultSpan>(0, text.length)
            for (span in textFindSpans) {
                text.removeSpan(span)
            }
            for (span in findResultSpans) {
                if (span.start >= 0 && span.end <= text.length && span.start <= span.end
                    && (span.start in lineStart..lineEnd || span.start <= lineEnd && span.end >= lineStart)) {
                    text.setSpan(
                        span,
                        if (span.start < lineStart) lineStart else span.start,
                        if (span.end > lineEnd) lineEnd else span.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    private fun syntaxHighlight() {
        cancelSyntaxHighlighting()
        syntaxScheme?.let {
            language?.runStyler(this, getProcessedText(), it)
        }
    }

    private fun cancelSyntaxHighlighting() {
        language?.cancelStyler()
    }

    private fun checkMatchingBracket(pos: Int) {
        if (layout != null) {
            if (openDelimiterSpan != null && closedDelimiterSpan != null) {
                text.removeSpan(openDelimiterSpan)
                text.removeSpan(closedDelimiterSpan)
            }
            if (config.highlightDelimiters) {
                if (pos > 0 && pos <= text.length) {
                    val c1 = text[pos - 1]
                    for (i in delimiters.indices) {
                        if (delimiters[i] == c1) {
                            val open = i <= 2
                            val c2 = delimiters[(i + 3) % 6]
                            var k = pos
                            if (open) {
                                var nob = 1
                                while (k < text.length) {
                                    if (text[k] == c2) {
                                        nob--
                                    }
                                    if (text[k] == c1) {
                                        nob++
                                    }
                                    if (nob == 0) {
                                        showBracket(pos - 1, k)
                                        break
                                    }
                                    k++
                                }
                            } else {
                                var ncb = 1
                                k -= 2
                                while (k >= 0) {
                                    if (text[k] == c2) {
                                        ncb--
                                    }
                                    if (text[k] == c1) {
                                        ncb++
                                    }
                                    if (ncb == 0) {
                                        showBracket(k, pos - 1)
                                        break
                                    }
                                    k--
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBracket(i: Int, j: Int) {
        if (openDelimiterSpan != null && closedDelimiterSpan != null) {
            text.setSpan(openDelimiterSpan, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(closedDelimiterSpan, j, j + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
