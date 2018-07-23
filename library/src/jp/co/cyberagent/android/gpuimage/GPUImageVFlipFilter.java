package jp.co.cyberagent.android.gpuimage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GPUImageVFlipFilter extends GPUImageFilter {

    private final FloatBuffer mGLTextureBuffer;

    public GPUImageVFlipFilter(){
        super();
        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        float[] vflip = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
        mGLTextureBuffer.put(vflip).position(0);
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, mGLTextureBuffer);
    }
}
