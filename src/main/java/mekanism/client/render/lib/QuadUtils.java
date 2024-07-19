package mekanism.client.render.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class QuadUtils {

    private QuadUtils() {
    }

    private static final float eps = 1F / 0x100;

    public static List<Quad> unpack(List<BakedQuad> quads) {
        return quads.stream().map(Quad::new).toList();
    }

    public static List<BakedQuad> bake(List<Quad> quads) {
        return quads.stream().map(Quad::bake).toList();
    }

    public static List<Quad> flip(List<Quad> quads) {
        return quads.stream().map(Quad::flip).toList();
    }

    public static List<Quad> transformQuads(List<Quad> orig, QuadTransformation transformation) {
        List<Quad> list = new ArrayList<>(orig.size());
        for (Quad quad : orig) {
            transformation.transform(quad);
            list.add(quad);
        }
        return list;
    }

    public static List<BakedQuad> transformBakedQuads(List<BakedQuad> orig, QuadTransformation transformation) {
        List<BakedQuad> list = new ArrayList<>(orig.size());
        for (BakedQuad bakedQuad : orig) {
            Quad quad = new Quad(bakedQuad);
            transformation.transform(quad);
            list.add(quad.bake());
        }
        return list;
    }

    public static List<BakedQuad> transformAndBake(List<Quad> orig, QuadTransformation transformation) {
        List<BakedQuad> list = new ArrayList<>(orig.size());
        for (Quad quad : orig) {
            transformation.transform(quad);
            list.add(quad.bake());
        }
        return list;
    }

    public static void remapUVs(Quad quad, TextureAtlasSprite newTexture) {
        float uMin = quad.getTexture().getU0(), uMax = quad.getTexture().getU1();
        float vMin = quad.getTexture().getV0(), vMax = quad.getTexture().getV1();
        for (Vertex v : quad.getVertices()) {
            float newU = (v.getTexU() - uMin) * 16F / (uMax - uMin);
            float newV = (v.getTexV() - vMin) * 16F / (vMax - vMin);
            v.texRaw(newTexture.getU(newU), newTexture.getV(newV));
        }
    }

    // this is an adaptation of fry's original UV contractor (pulled from BakedQuadBuilder).
    // ultimately this fixes UVs bleeding over the edge slightly when dealing with smaller models or tight UV bounds
    public static void contractUVs(Quad quad) {
        TextureAtlasSprite texture = quad.getTexture();
        float sizeX = texture.getWidth() / (texture.getU1() - texture.getU0());
        float sizeY = texture.getHeight() / (texture.getV1() - texture.getV0());
        float ep = 1F / (Math.max(sizeX, sizeY) * 0x100);
        float[] newUs = contract(quad, Vertex::getTexU, ep);
        float[] newVs = contract(quad, Vertex::getTexV, ep);
        for (int i = 0; i < quad.getVertices().length; i++) {
            quad.getVertices()[i].texRaw(newUs[i], newVs[i]);
        }
    }

    private static float[] contract(Quad quad, Function<Vertex, Float> uvf, float ep) {
        float center = 0;
        float[] ret = new float[4];
        for (int v = 0; v < 4; v++) {
            center += uvf.apply(quad.getVertices()[v]);
        }
        center /= 4;
        for (int v = 0; v < 4; v++) {
            float orig = uvf.apply(quad.getVertices()[v]);
            float shifted = orig * (1 - eps) + center * eps;
            float delta = orig - shifted;
            if (Math.abs(delta) < ep) { // not moving a fraction of a pixel
                float centerDelta = Math.abs(orig - center);
                if (centerDelta < 2 * ep) { // center is closer than 2 fractions of a pixel, don't move too close
                    shifted = (orig + center) / 2;
                } else { // move at least by a fraction
                    shifted = orig + (delta < 0 ? ep : -ep);
                }
            }
            ret[v] = shifted;
        }
        return ret;
    }
}
