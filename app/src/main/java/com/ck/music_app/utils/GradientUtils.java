package com.ck.music_app.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.List;

public class GradientUtils {
    public static void createGradientFromBitmap(Bitmap bitmap, View view) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                if (palette != null) {


                    List<Palette.Swatch> swatches = new ArrayList<>(palette.getSwatches());

                    if (swatches.size() >= 2) {
                        // Sắp xếp giảm dần theo population
                        swatches.sort((a, b) -> Integer.compare(b.getPopulation(), a.getPopulation()));
                        int[] colors = new int[]{swatches.get(2).getRgb(), swatches.get(0).getRgb()};
                        GradientDrawable gradientDrawable = new GradientDrawable(
                                GradientDrawable.Orientation.TL_BR,
                                colors
                        );
                        gradientDrawable.setAlpha(180);
                        view.setBackground(gradientDrawable);

                    }
                }
            }

        });
    }
}