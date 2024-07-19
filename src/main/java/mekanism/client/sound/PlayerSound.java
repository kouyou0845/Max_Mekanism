package mekanism.client.sound;

import java.lang.ref.WeakReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.config.MekanismConfig;
import mekanism.common.registration.impl.SoundEventRegistryObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public abstract class PlayerSound extends AbstractTickableSoundInstance {

    @Nonnull
    private final WeakReference<Player> playerReference;
    private final int subtitleFrequency;
    private float lastX;
    private float lastY;
    private float lastZ;

    private float fadeUpStep = 0.1f;
    private float fadeDownStep = 0.1f;
    private int consecutiveTicks;

    public PlayerSound(@Nonnull Player player, @Nonnull SoundEventRegistryObject<?> sound) {
        this(player, sound.get(), 60);
        //Set it to repeat the subtitle every 3 seconds the sound is constantly playing
    }

    public PlayerSound(@Nonnull Player player, @Nonnull SoundEvent sound, int subtitleFrequency) {
        super(sound, SoundSource.PLAYERS);
        this.playerReference = new WeakReference<>(player);
        this.subtitleFrequency = subtitleFrequency;
        this.lastX = (float) player.getX();
        this.lastY = (float) player.getY();
        this.lastZ = (float) player.getZ();
        this.looping = true;
        this.delay = 0;

        // N.B. the volume must be > 0 on first time it's processed by sound system or else it will not
        // get registered for tick events.
        this.volume = 0.1F;
    }

    @Nullable
    private Player getPlayer() {
        return playerReference.get();
    }

    protected void setFade(float fadeUpStep, float fadeDownStep) {
        this.fadeUpStep = fadeUpStep;
        this.fadeDownStep = fadeDownStep;
    }

    @Override
    public double getX() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        Player player = getPlayer();
        if (player != null) {
            this.lastX = (float) player.getX();
        }
        return this.lastX;
    }

    @Override
    public double getY() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        Player player = getPlayer();
        if (player != null) {
            this.lastY = (float) player.getY();
        }
        return this.lastY;
    }

    @Override
    public double getZ() {
        //Gracefully handle the player becoming null if this object is kept around after update marks us as donePlaying
        Player player = getPlayer();
        if (player != null) {
            this.lastZ = (float) player.getZ();
        }
        return this.lastZ;
    }

    @Override
    public void tick() {
        Player player = getPlayer();
        if (player == null || !player.isAlive()) {
            stop();
            volume = 0.0F;
            consecutiveTicks = 0;
            return;
        }

        if (shouldPlaySound(player)) {
            if (volume < 1.0F) {
                // If we weren't max volume, start fading up
                volume = Math.min(1.0F, volume + fadeUpStep);
            }
            if (consecutiveTicks % subtitleFrequency == 0) {
                SoundManager soundHandler = Minecraft.getInstance().getSoundManager();
                for (SoundEventListener soundEventListener : soundHandler.soundEngine.listeners) {
                    WeighedSoundEvents soundEventAccessor = resolve(soundHandler);
                    if (soundEventAccessor != null) {
                        soundEventListener.onPlaySound(this, soundEventAccessor);
                    }
                }
                consecutiveTicks = 1;
            } else {
                consecutiveTicks++;
            }
        } else if (volume > 0.0F) {
            consecutiveTicks = 0;
            // Not yet fully muted, fade down
            volume = Math.max(0.0F, volume - fadeDownStep);
        }
    }

    public abstract boolean shouldPlaySound(@Nonnull Player player);

    @Override
    public float getVolume() {
        return super.getVolume() * MekanismConfig.client.baseSoundVolume.get();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        Player player = getPlayer();
        if (player == null) {
            return super.canPlaySound();
        }
        return !player.isSilent();
    }

    public enum SoundType {
        FLAMETHROWER,
        JETPACK,
        SCUBA_MASK,
        GRAVITATIONAL_MODULATOR
    }
}