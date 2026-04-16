package de.tyro.mcnetwork.simulation;

import de.tyro.mcnetwork.client.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

public interface IHudRenderer {

    void render(RenderUtil renderUtil);

    Vec2 getRenderSize(Font font);
}
