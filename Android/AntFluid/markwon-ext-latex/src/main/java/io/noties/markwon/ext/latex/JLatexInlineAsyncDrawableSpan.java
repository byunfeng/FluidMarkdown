package io.noties.markwon.ext.latex;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.scilab.forge.jlatexmath.TeXIcon;

import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.image.AsyncDrawable;
import ru.noties.jlatexmath.JLatexMathDrawable;

/**
 * @since 4.3.0
 */
class JLatexInlineAsyncDrawableSpan extends JLatexAsyncDrawableSpan {

    private final AsyncDrawable drawable;

    JLatexInlineAsyncDrawableSpan(@NonNull MarkwonTheme theme, @NonNull JLatextAsyncDrawable drawable, @ColorInt int color) {
        super(theme, drawable, color, ALIGN_BASELINE);
        this.drawable = drawable;
    }

    @Override
    public int getSize(
            @NonNull Paint paint,
            CharSequence text,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            @Nullable Paint.FontMetricsInt fm) {

        // if we have no async drawable result - we will just render text

        final int size;

        if (drawable.hasResult()) {

            final Rect rect = drawable.getBounds();

            if (fm != null) {
                final Paint.FontMetricsInt paintFm = paint.getFontMetricsInt();
                final int drawableDepth = resolveDrawableDepth(rect);
                final int drawableAscent = rect.height() - drawableDepth;
                fm.ascent = Math.min(paintFm.ascent, -drawableAscent);
                fm.descent = Math.max(paintFm.descent, drawableDepth);
                fm.top = Math.min(paintFm.top, fm.ascent);
                fm.bottom = Math.max(paintFm.bottom, fm.descent);
            }

            size = rect.right;

        } else {

            // NB, no specific text handling (no new lines, etc)
            size = (int) (paint.measureText(text, start, end) + .5F);
        }

        return size;
    }

    @Override
    public void chooseHeight(
            CharSequence text,
            int start,
            int end,
            int spanstartv,
            int lineHeight,
            Paint.FontMetricsInt fm) {
        // Inline formulas should rely on metrics from getSize; image-style line-height expansion
        // introduces unnecessary extra leading for mixed text + formula lines.
    }

    @Override
    public void draw(
            @NonNull Canvas canvas,
            CharSequence text,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            float x,
            int top,
            int y,
            int bottom,
            @NonNull Paint paint) {
        if (drawable.hasResult()) {
            final Rect rect = drawable.getBounds();
            final int drawableDepth = resolveDrawableDepth(rect);
            final int drawableAscent = rect.height() - drawableDepth;
            final Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            final int desiredTop = y - drawableAscent;
            final int syntheticBottom = desiredTop + rect.height() + fm.bottom;
            super.draw(canvas, text, start, end, x, top, y, syntheticBottom, paint);
            return;
        }
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }

    private int resolveDrawableDepth(@NonNull Rect rect) {
        int depth = 0;
        final Drawable drawableResult = drawable.getResult();
        if (drawableResult instanceof JLatexMathDrawable) {
            final TeXIcon icon = ((JLatexMathDrawable) drawableResult).icon();
            final int iconHeight = icon.getIconHeight();
            if (iconHeight > 0) {
                depth = Math.round(icon.getIconDepth() * rect.height() / (float) iconHeight);
            } else {
                depth = icon.getIconDepth();
            }
        }
        if (depth < 0) {
            return 0;
        }
        return Math.min(rect.height(), depth);
    }
}
