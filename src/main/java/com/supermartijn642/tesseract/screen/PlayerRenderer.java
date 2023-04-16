package com.supermartijn642.tesseract.screen;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.gui.ScreenUtils;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Created 5/20/2021 by SuperMartijn642
 */
public class PlayerRenderer {

    // TODO: this should probably be cleared after a certain time
    private static final Map<UUID,GameProfile> PLAYER_PROFILE_MAP = new HashMap<>();
    private static final HashSet<UUID> FETCH_QUEUE = new HashSet<>();

    public static void renderPlayerHead(UUID player, PoseStack matrixStack, int x, int y, int width, int height){
        ScreenUtils.bindTexture(getPlayerSkin(player));
        ScreenUtils.drawTexture(matrixStack, x, y, width, height, 1 / 8f, 1 / 8f, 1 / 8f, 1 / 8f);
    }

    public static String getPlayerUsername(UUID player){
        GameProfile profile = fetchPlayerProfile(player);
        return profile == null ? null : profile.getName();
    }

    public static ResourceLocation getPlayerSkin(UUID player){
        GameProfile profile = fetchPlayerProfile(player);
        if(profile != null){
            SkinManager skinManager = ClientUtils.getMinecraft().getSkinManager();
            Map<MinecraftProfileTexture.Type,MinecraftProfileTexture> map = skinManager.getInsecureSkinInformation(profile);
            if(map.containsKey(MinecraftProfileTexture.Type.SKIN))
                return skinManager.registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
        }
        return DefaultPlayerSkin.getDefaultSkin(player);
    }

    private static GameProfile fetchPlayerProfile(final UUID player){
        synchronized(PLAYER_PROFILE_MAP){
            GameProfile profile = PLAYER_PROFILE_MAP.get(player);
            if(profile != null)
                return profile;
        }

        synchronized(FETCH_QUEUE){
            if(!FETCH_QUEUE.contains(player)){
                FETCH_QUEUE.add(player);
                new Thread(() -> {
                    boolean success = false;
                    String name = fetchPlayerName(player);
                    if(name != null){
                        GameProfile profile = updateGameProfile(new GameProfile(player, name));
                        if(profile != null){
                            synchronized(PLAYER_PROFILE_MAP){
                                PLAYER_PROFILE_MAP.put(player, profile);
                            }
                            success = true;
                        }
                    }
                    if(!success){
                        try{
                            Thread.sleep(120000);
                        }catch(Exception e2){
                            e2.printStackTrace();
                        }
                    }
                    synchronized(FETCH_QUEUE){
                        FETCH_QUEUE.remove(player);
                    }
                }, "Tesseract - UUID to username").start();
            }
        }

        return null;
    }

    @Nullable
    private static GameProfile updateGameProfile(@Nullable GameProfile input){
        if(input != null && input.getName() != null && !input.getName().isEmpty()){
            if(!input.isComplete() || !input.getProperties().containsKey("textures")){
                GameProfileCache profileCache = getProfileCache();
                MinecraftSessionService sessionService = getSessionService();
                if(profileCache != null && sessionService != null){
                    Optional<GameProfile> optionalGameProfile = profileCache.get(input.getName());
                    if(optionalGameProfile.isPresent()){
                        GameProfile gameProfile = optionalGameProfile.get();
                        Property property = Iterables.getFirst(gameProfile.getProperties().get("textures"), null);
                        if(property == null)
                            gameProfile = sessionService.fillProfileProperties(gameProfile, true);
                        return gameProfile;
                    }
                }
            }
        }
        return null;
    }

    private static String fetchPlayerName(UUID player){
        try{
            InputStream inputStream = new URL("https://api.mojang.com/user/profile/" + player).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String s;
            while((s = reader.readLine()) != null)
                builder.append(s);
            if(builder.length() > 0){
                // No tools to just read an array I guess
                JsonObject json = GsonHelper.parse(builder.toString());
                if(json.has("name"))
                    return json.get("name").getAsString();
            }
        }catch(Exception ignore){}
        return null;
    }

    private static GameProfileCache getProfileCache(){
        return ClientUtils.getMinecraft().getSingleplayerServer().getProfileCache();
    }

    private static MinecraftSessionService getSessionService(){
        return ClientUtils.getMinecraft().getMinecraftSessionService();
    }
}
