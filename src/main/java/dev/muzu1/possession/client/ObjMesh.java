package dev.muzu1.possession.client;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class ObjMesh {
    private final List<Quad> quads;

    private ObjMesh(List<Quad> quads) {
        this.quads = List.copyOf(quads);
    }

    static ObjMesh load(ResourceManager resourceManager, Identifier modelId) throws IOException {
        List<Vec3> positions = new ArrayList<>();
        List<Uv> uvs = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        List<FaceRef[]> faces = new ArrayList<>();

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resourceManager.getResource(modelId).orElseThrow().getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    positions.add(new Vec3(x, y, z));
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                    continue;
                }

                if (line.startsWith("vt ")) {
                    String[] parts = line.split("\\s+");
                    float u = Float.parseFloat(parts[1]);
                    float v = Float.parseFloat(parts[2]);
                    uvs.add(new Uv(u, 1.0F - v));
                    continue;
                }

                if (line.startsWith("vn ")) {
                    String[] parts = line.split("\\s+");
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    normals.add(new Vec3(x, y, z));
                    continue;
                }

                if (line.startsWith("f ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    if (parts.length < 3) {
                        continue;
                    }

                    FaceRef[] refs = new FaceRef[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        String[] indices = parts[i].split("/");
                        int positionIndex = parseIndex(indices, 0);
                        int uvIndex = parseIndex(indices, 1);
                        int normalIndex = parseIndex(indices, 2);
                        refs[i] = new FaceRef(positionIndex, uvIndex, normalIndex);
                    }
                    faces.add(refs);
                }
            }
        }

        if (positions.isEmpty() || faces.isEmpty()) {
            return new ObjMesh(List.of());
        }

        float centerX = (minX + maxX) * 0.5F;
        float centerZ = (minZ + maxZ) * 0.5F;
        float topY = maxY;

        List<Quad> quads = new ArrayList<>(faces.size());
        for (FaceRef[] refs : faces) {
            if (refs.length == 4) {
                quads.add(new Quad(buildVertex(refs[0], positions, uvs, normals, centerX, topY, centerZ),
                        buildVertex(refs[1], positions, uvs, normals, centerX, topY, centerZ),
                        buildVertex(refs[2], positions, uvs, normals, centerX, topY, centerZ),
                        buildVertex(refs[3], positions, uvs, normals, centerX, topY, centerZ)));
            } else {
                for (int i = 1; i < refs.length - 1; i++) {
                    quads.add(new Quad(buildVertex(refs[0], positions, uvs, normals, centerX, topY, centerZ),
                            buildVertex(refs[i], positions, uvs, normals, centerX, topY, centerZ),
                            buildVertex(refs[i + 1], positions, uvs, normals, centerX, topY, centerZ),
                            buildVertex(refs[i + 1], positions, uvs, normals, centerX, topY, centerZ)));
                }
            }
        }

        return new ObjMesh(quads);
    }

    void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        for (Quad quad : quads) {
            quad.emit(vertexConsumer, positionMatrix, normalMatrix, light);
        }
    }

    private static Vertex buildVertex(FaceRef faceRef,
                                      List<Vec3> positions,
                                      List<Uv> uvs,
                                      List<Vec3> normals,
                                      float centerX,
                                      float topY,
                                      float centerZ) {
        Vec3 position = faceRef.positionIndex >= 0 && faceRef.positionIndex < positions.size()
                ? positions.get(faceRef.positionIndex)
                : Vec3.ZERO;
        Uv uv = faceRef.uvIndex >= 0 && faceRef.uvIndex < uvs.size()
                ? uvs.get(faceRef.uvIndex)
                : Uv.ZERO;
        Vec3 normal = faceRef.normalIndex >= 0 && faceRef.normalIndex < normals.size()
                ? normals.get(faceRef.normalIndex)
                : Vec3.FORWARD;

        return new Vertex(
                position.x - centerX,
                position.y - topY,
                position.z - centerZ,
                uv.u,
                uv.v,
                normal.x,
                normal.y,
                normal.z
        );
    }

    private static int parseIndex(String[] indices, int part) {
        if (part >= indices.length || indices[part].isEmpty()) {
            return -1;
        }
        return Math.max(-1, Integer.parseInt(indices[part]) - 1);
    }

    private record FaceRef(int positionIndex, int uvIndex, int normalIndex) {
    }

    private record Vec3(float x, float y, float z) {
        private static final Vec3 ZERO = new Vec3(0.0F, 0.0F, 0.0F);
        private static final Vec3 FORWARD = new Vec3(0.0F, 0.0F, 1.0F);
    }

    private record Uv(float u, float v) {
        private static final Uv ZERO = new Uv(0.0F, 0.0F);
    }

    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        void emit(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, int light) {
            float normalLength = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
            float normalX = normalLength > 0.0F ? nx / normalLength : 0.0F;
            float normalY = normalLength > 0.0F ? ny / normalLength : 0.0F;
            float normalZ = normalLength > 0.0F ? nz / normalLength : 1.0F;

            vertexConsumer.vertex(positionMatrix, x, y, z)
                    .color(255, 255, 255, 255)
                    .texture(u, v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(normalMatrix, normalX, normalY, normalZ)
                    .next();
        }
    }

    private record Quad(Vertex first, Vertex second, Vertex third, Vertex fourth) {
        void emit(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, int light) {
            first.emit(vertexConsumer, positionMatrix, normalMatrix, light);
            second.emit(vertexConsumer, positionMatrix, normalMatrix, light);
            third.emit(vertexConsumer, positionMatrix, normalMatrix, light);
            fourth.emit(vertexConsumer, positionMatrix, normalMatrix, light);
        }
    }
}
