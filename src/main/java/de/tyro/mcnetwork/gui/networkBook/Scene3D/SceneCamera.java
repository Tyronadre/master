package de.tyro.mcnetwork.gui.networkBook.Scene3D;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SceneCamera {
    private float yaw = 45;
    private float pitch = 30;
    private float distance = 6;

    public void orbit(float dx, float dy) {
        yaw += dx * 0.3f;
        pitch -= dy * 0.3f;
        pitch = Math.max(-85, Math.min(85, pitch));
    }

    public void zoom(float delta) {
        distance *= (1f - delta * 0.1f);
        distance = Math.max(2f, Math.min(20f, distance));
    }

    public void update() {
        // could interpolate or add smooth damping
    }

    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f(0, 1.0f, 0);
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        Vector3f eye = new Vector3f(
                (float)(distance * Math.cos(pitchRad) * Math.cos(yawRad)),
                (float)(distance * Math.sin(pitchRad)),
                (float)(distance * Math.cos(pitchRad) * Math.sin(yawRad))
        ).add(target);

        return new Matrix4f().lookAt(eye, target, new Vector3f(0, 1, 0));
    }
}
