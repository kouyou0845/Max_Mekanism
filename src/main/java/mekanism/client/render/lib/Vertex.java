package mekanism.client.render.lib;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import mekanism.common.lib.Color;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

public class Vertex {

    // I'm not sure why Forge packs light this way but w/e
    private static final float LIGHT_PACK_FACTOR = 240F / Short.MAX_VALUE;

    private Vec3 pos;
    private Vec3 normal;

    private Color color;

    // 0 to 16
    private float texU, texV;
    // 0 to 1
    private float lightU, lightV;

    public Vertex() {
    }

    public Vertex(Vec3 pos, Vec3 normal, Color color, float texU, float texV, float lightU, float lightV) {
        this.pos = pos;
        this.normal = normal;
        this.color = color;
        this.texU = texU;
        this.texV = texV;
        this.lightU = lightU;
        this.lightV = lightV;
    }

    public static Vertex create(Vec3 pos, Vec3 normal, Color color, TextureAtlasSprite sprite, float texU, float texV, float lightU, float lightV) {
        return new Vertex(pos, normal, color, sprite.getU(texU), sprite.getV(texV), lightU, lightV);
    }

    public static Vertex create(Vec3 pos, Vec3 normal, TextureAtlasSprite sprite, float u, float v) {
        return create(pos, normal, Color.WHITE, sprite, u, v, 0, 0);
    }

    public Vec3 getPos() {
        return pos;
    }

    public Vec3 getNormal() {
        return normal;
    }

    public Color getColor() {
        return color;
    }

    public float getTexU() {
        return texU;
    }

    public float getTexV() {
        return texV;
    }

    public float getLightU() {
        return lightU;
    }

    public float getLightV() {
        return lightV;
    }

    public Vertex color(Color color) {
        this.color = color;
        return this;
    }

    public Vertex pos(Vec3 pos) {
        this.pos = pos;
        return this;
    }

    public Vertex normal(Vec3 normal) {
        this.normal = normal;
        return this;
    }

    public Vertex texRaw(float u, float v) {
        texU = u;
        texV = v;
        return this;
    }

    public Vertex lightRaw(float u, float v) {
        lightU = u;
        lightV = v;
        return this;
    }

    public Vertex light(float u, float v) {
        return lightRaw(u * LIGHT_PACK_FACTOR, v * LIGHT_PACK_FACTOR);
    }

    public Vertex copy() {
        return new Vertex(pos, normal, color, texU, texV, lightU, lightV);
    }

    public float[][] pack(VertexFormat format) {
        float[][] ret = new float[format.getElements().size()][4];
        for (int i = 0; i < format.getElements().size(); i++) {
            VertexFormatElement element = format.getElements().get(i);
            switch (element.getUsage()) {
                case POSITION -> {
                    ret[i][0] = (float) pos.x();
                    ret[i][1] = (float) pos.y();
                    ret[i][2] = (float) pos.z();
                }
                case NORMAL -> {
                    ret[i][0] = (float) normal.x();
                    ret[i][1] = (float) normal.y();
                    ret[i][2] = (float) normal.z();
                }
                case COLOR -> {
                    ret[i][0] = color.rf();
                    ret[i][1] = color.gf();
                    ret[i][2] = color.bf();
                    ret[i][3] = color.af();
                }
                case UV -> {
                    if (element.getIndex() == 0) {
                        ret[i][0] = texU;
                        ret[i][1] = texV;
                    } else if (element.getIndex() == 2) {
                        ret[i][0] = lightU;
                        ret[i][1] = lightV;
                    }
                }
            }
        }
        return ret;
    }
}
