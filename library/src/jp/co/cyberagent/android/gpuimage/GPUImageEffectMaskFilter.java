package jp.co.cyberagent.android.gpuimage;

import android.graphics.Bitmap;

import java.nio.FloatBuffer;

public class GPUImageEffectMaskFilter extends GPUImageFilterGroup {

    private final GPUImageThreeInputAlphaBlendFilter mAlphaBlendFilter;

    public GPUImageEffectMaskFilter() {
        mAlphaBlendFilter = new GPUImageThreeInputAlphaBlendFilter();
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
        mAlphaBlendFilter.setSecondTexture(getLastDrawnTexture(), getLastDrawnTextureBuffer());
        mAlphaBlendFilter.onDraw(textureId, cubeBuffer, textureBuffer);
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        mAlphaBlendFilter.onOutputSizeChanged(width, height);
    }

    @Override
    public void onInit() {
        super.onInit();
        mAlphaBlendFilter.init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlphaBlendFilter.destroy();
    }

    public void setBitmap(final Bitmap bitmap) {
        mAlphaBlendFilter.setBitmap(bitmap);
    }

    public Bitmap getBitmap() {
        return mAlphaBlendFilter.getBitmap();
    }

    public void recycleBitmap() {
        mAlphaBlendFilter.recycleBitmap();
    }
}
