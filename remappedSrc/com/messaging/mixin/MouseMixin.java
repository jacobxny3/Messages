package com.messaging.mixin;

import com.messaging.client.MessagingModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (action == 1) { // Button pressed
            MouseHandler mouse = (MouseHandler) (Object) this;
            Minecraft client = Minecraft.getInstance();
            double mouseX = mouse.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getScreenWidth();
            double mouseY = mouse.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) client.getWindow().getScreenHeight();

            // Check if notification was clicked
            if (MessagingModClient.getNotificationRenderer().mouseClicked(mouseX, mouseY, action)) {
                ci.cancel();
            }
        }
    }
}