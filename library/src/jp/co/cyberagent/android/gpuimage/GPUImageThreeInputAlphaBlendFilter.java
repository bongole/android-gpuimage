package jp.co.cyberagent.android.gpuimage;

public class GPUImageThreeInputAlphaBlendFilter extends GPUImageThreeInputFilter {
        public static final String ALPHA_BLEND_FRAGMENT_SHADER = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            " varying highp vec2 textureCoordinate3;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " uniform sampler2D inputImageTexture3;\n" +
            "\n" +
            " void main()\n" +
            " {\n" +
            "   lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "   lowp vec4 textureColor3 = texture2D(inputImageTexture3, textureCoordinate3);\n" +
            "\n" +
            "   gl_FragColor = vec4(mix(textureColor.rgb, textureColor2.rgb, textureColor3.a), textureColor.a);\n" +
            " }";

        public GPUImageThreeInputAlphaBlendFilter(){
            super(ALPHA_BLEND_FRAGMENT_SHADER);
        }
}
