package com.supermartijn642.tesseract.screen.info;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.core.gui.ScreenUtils;
import com.supermartijn642.core.gui.widget.AbstractButtonWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created 4/18/2021 by SuperMartijn642
 */
public class InfoPageButton extends AbstractButtonWidget {

    private static final ResourceLocation BUTTONS = new ResourceLocation("tesseract", "textures/gui/page_navigation.png");

    private final int pageIndex;
    private final Supplier<Integer> currentPage;

    public InfoPageButton(int x, int y, int width, int height, int pageIndex, Supplier<Integer> currentPage, Consumer<Integer> setPage){
        super(x, y, width, height, () -> setPage.accept(pageIndex));
        this.pageIndex = pageIndex;
        this.currentPage = currentPage;
    }

    @Override
    protected Component getNarrationMessage(){
        return new TranslatableComponent("gui.tesseract.info.narrate_page", this.pageIndex);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks){
        ScreenUtils.bindTexture(BUTTONS);
        boolean selected = this.currentPage.get() == this.pageIndex;
        ScreenUtils.drawTexture(matrixStack, this.x, this.y, this.width, this.height, 7 / 18f, (this.active ? selected ? 2 : this.hovered ? 1 : 0 : 3) / 4f, 4 / 18f, 1 / 4f);
    }
}
