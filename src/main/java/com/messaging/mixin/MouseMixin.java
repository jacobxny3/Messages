package com.messaging.mixin;

import com.messaging.client.MessagingModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == 1) { // Button pressed
            Mouse mouse = (Mouse) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();
            double mouseX = mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
            double mouseY = mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();

            // Check if notification was clicked
            if (MessagingModClient.getNotificationRenderer().mouseClicked(mouseX, mouseY, action)) {
                ci.cancel();
            }
        }
    }
}