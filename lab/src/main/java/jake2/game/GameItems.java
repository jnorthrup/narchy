/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/




package jake2.game;


import jake2.Defines;
import jake2.qcommon.Com;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.stream.Stream;


public class GameItems {

    public static final gitem_armor_t jacketarmor_info = new gitem_armor_t(25, 50,
    .30f, .00f, Defines.ARMOR_JACKET);
    public static final gitem_armor_t combatarmor_info = new gitem_armor_t(50, 100,
    .60f, .30f, Defines.ARMOR_COMBAT);
    public static final gitem_armor_t bodyarmor_info = new gitem_armor_t(100, 200,
    .80f, .60f, Defines.ARMOR_BODY);
    static int quad_drop_timeout_hack;
    static int jacket_armor_index;
    static int combat_armor_index;
    static int body_armor_index;
    static int power_screen_index;
    static int power_shield_index;
    
    static final EntThinkAdapter DoRespawn = new EntThinkAdapter() {
        @Override
        public String getID() { return "do_respawn";}
        @Override
        public boolean think(edict_t ent) {
            if (ent.team != null) {
                edict_t master;
                int count;
                int choice = 0;
    
                master = ent.teammaster;
    
                
                for (count = 0, ent = master; ent != null; ent = ent.chain, count++)
                    ;
    
                choice = Lib.rand() % count;
    
                for (count = 0, ent = master; count < choice; ent = ent.chain, count++)
                    ;
            }
    
            ent.svflags &= ~Defines.SVF_NOCLIENT;
            ent.solid = Defines.SOLID_TRIGGER;
            game_import_t.linkentity(ent);
    
            
            ent.s.event = Defines.EV_ITEM_RESPAWN;
    
            return false;
        }
    };
    static final EntInteractAdapter Pickup_Pack = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_pack";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
    
            gitem_t item;
            int index;
    
            if (other.client.pers.max_bullets < 300)
                other.client.pers.max_bullets = 300;
            if (other.client.pers.max_shells < 200)
                other.client.pers.max_shells = 200;
            if (other.client.pers.max_rockets < 100)
                other.client.pers.max_rockets = 100;
            if (other.client.pers.max_grenades < 100)
                other.client.pers.max_grenades = 100;
            if (other.client.pers.max_cells < 300)
                other.client.pers.max_cells = 300;
            if (other.client.pers.max_slugs < 100)
                other.client.pers.max_slugs = 100;
    
            item = FindItem("Bullets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_bullets)
                    other.client.pers.inventory[index] = other.client.pers.max_bullets;
            }
    
            item = FindItem("Shells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_shells)
                    other.client.pers.inventory[index] = other.client.pers.max_shells;
            }
    
            item = FindItem("Cells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_cells)
                    other.client.pers.inventory[index] = other.client.pers.max_cells;
            }
    
            item = FindItem("Grenades");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_grenades)
                    other.client.pers.inventory[index] = other.client.pers.max_grenades;
            }
    
            item = FindItem("Rockets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_rockets)
                    other.client.pers.inventory[index] = other.client.pers.max_rockets;
            }
    
            item = FindItem("Slugs");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_slugs)
                    other.client.pers.inventory[index] = other.client.pers.max_slugs;
            }
    
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
        }
    };
    final static EntInteractAdapter Pickup_Health = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_health";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
    
            if (0 == (ent.style & Defines.HEALTH_IGNORE_MAX))
                if (other.health >= other.max_health)
                    return false;
    
            other.health += ent.count;
    
            if (0 == (ent.style & Defines.HEALTH_IGNORE_MAX)) {
                if (other.health > other.max_health)
                    other.health = other.max_health;
            }
    
            if (0 != (ent.style & Defines.HEALTH_TIMED)) {
                ent.think = GameUtil.MegaHealth_think;
                ent.nextthink = GameBase.level.time + 5f;
                ent.owner = other;
                ent.flags |= Defines.FL_RESPAWN;
                ent.svflags |= Defines.SVF_NOCLIENT;
                ent.solid = Defines.SOLID_NOT;
            } else {
                if ((ent.spawnflags & Defines.DROPPED_ITEM) == 0
                        && (GameBase.deathmatch.value != 0))
                    SetRespawn(ent, 30);
            }
    
            return true;
        }
    
    };
    static final EntTouchAdapter Touch_Item = new EntTouchAdapter() {
        @Override
        public String getID() { return "touch_item";}
        @Override
        public void touch(edict_t ent, edict_t other, cplane_t plane,
                          csurface_t surf) {
            boolean taken;
    
            if (ent.classname.equals("item_breather"))
                taken = false;
    
            if (other.client == null)
                return;
            if (other.health < 1)
                return; 
            if (ent.item.pickup == null)
                return; 
    
            taken = ent.item.pickup.interact(ent, other);
    
            if (taken) {
                
                other.client.bonus_alpha = 0.25f;
    
                
                other.client.ps.stats[Defines.STAT_PICKUP_ICON] = (short) game_import_t
                        .imageindex(ent.item.icon);
                other.client.ps.stats[Defines.STAT_PICKUP_STRING] = (short) (Defines.CS_ITEMS + ITEM_INDEX(ent.item));
                other.client.pickup_msg_time = GameBase.level.time + 3.0f;
    
                
                if (ent.item.use != null)
                    other.client.pers.selected_item = other.client.ps.stats[Defines.STAT_SELECTED_ITEM] = (short) ITEM_INDEX(ent.item);
    
                if (ent.item.pickup == Pickup_Health) {
                    if (ent.count == 2)
                        game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                                .soundindex("items/s_health.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else if (ent.count == 10)
                        game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                                .soundindex("items/n_health.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else if (ent.count == 25)
                        game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                                .soundindex("items/l_health.wav"), 1,
                                Defines.ATTN_NORM, 0);
                    else
                        
                        game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                                .soundindex("items/m_health.wav"), 1,
                                Defines.ATTN_NORM, 0);
                } else if (ent.item.pickup_sound != null) {
                    game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                            .soundindex(ent.item.pickup_sound), 1,
                            Defines.ATTN_NORM, 0);
                }
            }
    
            if (0 == (ent.spawnflags & Defines.ITEM_TARGETS_USED)) {
                GameUtil.G_UseTargets(ent, other);
                ent.spawnflags |= Defines.ITEM_TARGETS_USED;
            }
    
            if (!taken)
                return;
            
            Com.dprintln("Picked up:" + ent.classname);
    
            if (!((GameBase.coop.value != 0) && (ent.item.flags & Defines.IT_STAY_COOP) != 0)
                    || 0 != (ent.spawnflags & (Defines.DROPPED_ITEM | Defines.DROPPED_PLAYER_ITEM))) {
                if ((ent.flags & Defines.FL_RESPAWN) != 0)
                    ent.flags &= ~Defines.FL_RESPAWN;
                else
                    GameUtil.G_FreeEdict(ent);
            }
        }
    };
    static final EntTouchAdapter drop_temp_touch = new EntTouchAdapter() {
        @Override
        public String getID() { return "drop_temp_touch";}
        @Override
        public void touch(edict_t ent, edict_t other, cplane_t plane,
                          csurface_t surf) {
            if (other == ent.owner)
                return;
    
            Touch_Item.touch(ent, other, plane, surf);
        }
    };
    static final EntThinkAdapter drop_make_touchable = new EntThinkAdapter() {
        @Override
        public String getID() { return "drop_make_touchable";}
        @Override
        public boolean think(edict_t ent) {
            ent.touch = Touch_Item;
            if (GameBase.deathmatch.value != 0) {
                ent.nextthink = GameBase.level.time + 29;
                ent.think = GameUtil.G_FreeEdictA;
            }
            return false;
        }
    };
    static final ItemUseAdapter Use_Quad = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_quad";}
        @Override
        public void use(edict_t ent, gitem_t item) {
            int timeout;
    
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (quad_drop_timeout_hack != 0) {
                timeout = quad_drop_timeout_hack;
                quad_drop_timeout_hack = 0;
            } else {
                timeout = 300;
            }
    
            if (ent.client.quad_framenum > GameBase.level.framenum)
                ent.client.quad_framenum += timeout;
            else
                ent.client.quad_framenum = GameBase.level.framenum + timeout;
    
            game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                    .soundindex("items/damage.wav"), 1, Defines.ATTN_NORM, 0);
        }
    };
    
    static final ItemUseAdapter Use_Invulnerability = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_invulnerability";}
        @Override
        public void use(edict_t ent, gitem_t item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.invincible_framenum > GameBase.level.framenum)
                ent.client.invincible_framenum += 300;
            else
                ent.client.invincible_framenum = GameBase.level.framenum + 300;
    
            game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                    .soundindex("items/protect.wav"), 1, Defines.ATTN_NORM, 0);
        }
    };
    static final ItemUseAdapter Use_Breather = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_breather";}
        @Override
        public void use(edict_t ent, gitem_t item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
    
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.breather_framenum > GameBase.level.framenum)
                ent.client.breather_framenum += 300;
            else
                ent.client.breather_framenum = GameBase.level.framenum + 300;
    
            game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                    .soundindex("items/damage.wav"), 1, Defines.ATTN_NORM, 0);
        }
    };
    static final ItemUseAdapter Use_Envirosuit = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_envirosuit";}
        @Override
        public void use(edict_t ent, gitem_t item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.enviro_framenum > GameBase.level.framenum)
                ent.client.enviro_framenum += 300;
            else
                ent.client.enviro_framenum = GameBase.level.framenum + 300;
    
            game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                    .soundindex("items/damage.wav"), 1, Defines.ATTN_NORM, 0);
        }
    };
    static final ItemUseAdapter Use_Silencer = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_silencer";}
        @Override
        public void use(edict_t ent, gitem_t item) {
    
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
            ent.client.silencer_shots += 30;
    
            game_import_t.sound(ent, Defines.CHAN_ITEM, game_import_t
                    .soundindex("items/damage.wav"), 1, Defines.ATTN_NORM, 0);
        }
    };
    static final EntInteractAdapter Pickup_Key = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_key";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            if (GameBase.coop.value != 0) {
                if (Lib.strcmp(ent.classname, "key_power_cube") == 0) {
                    if ((other.client.pers.power_cubes & ((ent.spawnflags & 0x0000ff00) >> 8)) != 0)
                        return false;
                    other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
                    other.client.pers.power_cubes |= ((ent.spawnflags & 0x0000ff00) >> 8);
                } else {
                    if (other.client.pers.inventory[ITEM_INDEX(ent.item)] != 0)
                        return false;
                    other.client.pers.inventory[ITEM_INDEX(ent.item)] = 1;
                }
                return true;
            }
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_Ammo = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_ammo";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            int oldcount;
            int count;
            boolean weapon;
    
            weapon = (ent.item.flags & Defines.IT_WEAPON) != 0;
            if ((weapon)
                    && ((int) GameBase.dmflags.value & Defines.DF_INFINITE_AMMO) != 0)
                count = 1000;
            else if (ent.count != 0)
                count = ent.count;
            else
                count = ent.item.quantity;
    
            oldcount = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
            if (!Add_Ammo(other, ent.item, count))
                return false;
    
            if (weapon && 0 == oldcount) {
                if (other.client.pers.weapon != ent.item
                        && (0 == GameBase.deathmatch.value || other.client.pers.weapon == FindItem("blaster")))
                    other.client.newweapon = ent.item;
            }
    
            if (0 == (ent.spawnflags & (Defines.DROPPED_ITEM | Defines.DROPPED_PLAYER_ITEM))
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, 30);
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_Armor = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_armor";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            int old_armor_index;
            gitem_armor_t oldinfo;
            gitem_armor_t newinfo;
            int newcount;
            float salvage;
            int salvagecount;
    
            
            newinfo = ent.item.info;
    
            old_armor_index = ArmorIndex(other);
    
            
            if (ent.item.tag == Defines.ARMOR_SHARD) {
                if (0 == old_armor_index)
                    other.client.pers.inventory[jacket_armor_index] = 2;
                else
                    other.client.pers.inventory[old_armor_index] += 2;
            }
    
            
            else if (0 == old_armor_index) {
                other.client.pers.inventory[ITEM_INDEX(ent.item)] = newinfo.base_count;
            }
    
            
            else {
                
                if (old_armor_index == jacket_armor_index)
                    oldinfo = jacketarmor_info;
    
                else if (old_armor_index == combat_armor_index)
                    oldinfo = combatarmor_info;
    
                else
                    
                    oldinfo = bodyarmor_info;
    
                if (newinfo.normal_protection > oldinfo.normal_protection) {
                    
                    salvage = oldinfo.normal_protection
                            / newinfo.normal_protection;
                    salvagecount = (int) salvage
                            * other.client.pers.inventory[old_armor_index];
                    newcount = newinfo.base_count + salvagecount;
                    if (newcount > newinfo.max_count)
                        newcount = newinfo.max_count;
    
                    
                    other.client.pers.inventory[old_armor_index] = 0;
    
                    
                    other.client.pers.inventory[ITEM_INDEX(ent.item)] = newcount;
                } else {
                    
                    salvage = newinfo.normal_protection
                            / oldinfo.normal_protection;
                    salvagecount = (int) salvage * newinfo.base_count;
                    newcount = other.client.pers.inventory[old_armor_index]
                            + salvagecount;
                    if (newcount > oldinfo.max_count)
                        newcount = oldinfo.max_count;
    
                    
                    
                    if (other.client.pers.inventory[old_armor_index] >= newcount)
                        return false;
    
                    
                    other.client.pers.inventory[old_armor_index] = newcount;
                }
            }
    
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, 20);
    
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_PowerArmor = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_powerarmor";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
    
            int quantity;
    
            quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
            if (GameBase.deathmatch.value != 0) {
                if (0 == (ent.spawnflags & Defines.DROPPED_ITEM))
                    SetRespawn(ent, ent.item.quantity);
                
                if (0 == quantity)
                    ent.item.use.use(other, ent.item);
            }
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_Powerup = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_powerup";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            int quantity;
    
            quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
            if ((GameBase.skill.value == 1 && quantity >= 2)
                    || (GameBase.skill.value >= 2 && quantity >= 1))
                return false;
    
            if ((GameBase.coop.value != 0)
                    && (ent.item.flags & Defines.IT_STAY_COOP) != 0
                    && (quantity > 0))
                return false;
    
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
            if (GameBase.deathmatch.value != 0) {
                if (0 == (ent.spawnflags & Defines.DROPPED_ITEM))
                    SetRespawn(ent, ent.item.quantity);
                if (((int) GameBase.dmflags.value & Defines.DF_INSTANT_ITEMS) != 0
                        || ((ent.item.use == Use_Quad) && 0 != (ent.spawnflags & Defines.DROPPED_PLAYER_ITEM))) {
                    if ((ent.item.use == Use_Quad)
                            && 0 != (ent.spawnflags & Defines.DROPPED_PLAYER_ITEM))
                        quad_drop_timeout_hack = (int) ((ent.nextthink - GameBase.level.time) / Defines.FRAMETIME);
    
                    ent.item.use.use(other, ent.item);
                }
            }
    
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_Adrenaline = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_adrenaline";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            if (GameBase.deathmatch.value == 0)
                other.max_health += 1;
    
            if (other.health < other.max_health)
                other.health = other.max_health;
    
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
    
        }
    };
    public static final EntInteractAdapter Pickup_AncientHead = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_ancienthead";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            other.max_health += 2;
    
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
        }
    };
    public static final EntInteractAdapter Pickup_Bandolier = new EntInteractAdapter() {
        @Override
        public String getID() { return "pickup_bandolier";}
        @Override
        public boolean interact(edict_t ent, edict_t other) {
            gitem_t item;
            int index;
    
            if (other.client.pers.max_bullets < 250)
                other.client.pers.max_bullets = 250;
            if (other.client.pers.max_shells < 150)
                other.client.pers.max_shells = 150;
            if (other.client.pers.max_cells < 250)
                other.client.pers.max_cells = 250;
            if (other.client.pers.max_slugs < 75)
                other.client.pers.max_slugs = 75;
    
            item = FindItem("Bullets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_bullets)
                    other.client.pers.inventory[index] = other.client.pers.max_bullets;
            }
    
            item = FindItem("Shells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_shells)
                    other.client.pers.inventory[index] = other.client.pers.max_shells;
            }
    
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
    
        }
    };
    public static final ItemDropAdapter Drop_Ammo = new ItemDropAdapter() {
        @Override
        public String getID() { return "drop_ammo";}
        @Override
        public void drop(edict_t ent, gitem_t item) {
            edict_t dropped;
            int index;
    
            index = ITEM_INDEX(item);
            dropped = Drop_Item(ent, item);
            if (ent.client.pers.inventory[index] >= item.quantity)
                dropped.count = item.quantity;
            else
                dropped.count = ent.client.pers.inventory[index];
    
            if (ent.client.pers.weapon != null
                    && ent.client.pers.weapon.tag == Defines.AMMO_GRENADES
                    && item.tag == Defines.AMMO_GRENADES
                    && ent.client.pers.inventory[index] - dropped.count <= 0) {
                game_import_t.cprintf(ent, Defines.PRINT_HIGH,
                        "Can't drop current weapon\n");
                GameUtil.G_FreeEdict(dropped);
                return;
            }
    
            ent.client.pers.inventory[index] -= dropped.count;
            Cmd.ValidateSelectedItem(ent);
        }
    };
    public static final ItemDropAdapter Drop_General = new ItemDropAdapter() {
        @Override
        public String getID() { return "drop_general";}
        @Override
        public void drop(edict_t ent, gitem_t item) {
            Drop_Item(ent, item);
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            Cmd.ValidateSelectedItem(ent);
        }
    };
    
    public static final ItemDropAdapter Drop_PowerArmor = new ItemDropAdapter() {
        @Override
        public String getID() { return "drop_powerarmor";}
        @Override
        public void drop(edict_t ent, gitem_t item) {
            if (0 != (ent.flags & Defines.FL_POWER_ARMOR)
                    && (ent.client.pers.inventory[ITEM_INDEX(item)] == 1))
                Use_PowerArmor.use(ent, item);
            Drop_General.drop(ent, item);
        }
    };
    
    public static final EntThinkAdapter droptofloor = new EntThinkAdapter() {
        @Override
        public String getID() { return "drop_to_floor";}
        @Override
        public boolean think(edict_t ent) {
            trace_t tr;
            float[] dest = { 0, 0, 0 };
    
            
    
            
            
            ent.mins[0] = ent.mins[1] = ent.mins[2] = -15;
            
            
            ent.maxs[0] = ent.maxs[1] = ent.maxs[2] = 15;
    
            if (ent.model != null)
                game_import_t.setmodel(ent, ent.model);
            else
                game_import_t.setmodel(ent, ent.item.world_model);
            ent.solid = Defines.SOLID_TRIGGER;
            ent.movetype = Defines.MOVETYPE_TOSS;
            ent.touch = Touch_Item;

            float[] v = {0, 0, -128};
            Math3D.VectorAdd(ent.s.origin, v, dest);
    
            tr = game_import_t.trace(ent.s.origin, ent.mins, ent.maxs, dest, ent,
                    Defines.MASK_SOLID);
            if (tr.startsolid) {
                game_import_t.dprintf("droptofloor: " + ent.classname
                        + " startsolid at " + Lib.vtos(ent.s.origin) + '\n');
                GameUtil.G_FreeEdict(ent);
                return true;
            }
    
            Math3D.VectorCopy(tr.endpos, ent.s.origin);
    
            if (ent.team != null) {
                ent.flags &= ~Defines.FL_TEAMSLAVE;
                ent.chain = ent.teamchain;
                ent.teamchain = null;
    
                ent.svflags |= Defines.SVF_NOCLIENT;
                ent.solid = Defines.SOLID_NOT;
                if (ent == ent.teammaster) {
                    ent.nextthink = GameBase.level.time + Defines.FRAMETIME;
                    ent.think = DoRespawn;
                }
            }
    
            if ((ent.spawnflags & Defines.ITEM_NO_TOUCH) != 0) {
                ent.solid = Defines.SOLID_BBOX;
                ent.touch = null;
                ent.s.effects &= ~Defines.EF_ROTATE;
                ent.s.renderfx &= ~Defines.RF_GLOW;
            }
    
            if ((ent.spawnflags & Defines.ITEM_TRIGGER_SPAWN) != 0) {
                ent.svflags |= Defines.SVF_NOCLIENT;
                ent.solid = Defines.SOLID_NOT;
                ent.use = Use_Item;
            }
    
            game_import_t.linkentity(ent);
            return true;
        }
    };
    public static final ItemUseAdapter Use_PowerArmor = new ItemUseAdapter() {
        @Override
        public String getID() { return "use_powerarmor";}
        @Override
        public void use(edict_t ent, gitem_t item) {
            int index;
    
            if ((ent.flags & Defines.FL_POWER_ARMOR) != 0) {
                ent.flags &= ~Defines.FL_POWER_ARMOR;
                game_import_t
                        .sound(ent, Defines.CHAN_AUTO, game_import_t
                                .soundindex("misc/power2.wav"), 1,
                                Defines.ATTN_NORM, 0);
            } else {
                index = ITEM_INDEX(FindItem("cells"));
                if (0 == ent.client.pers.inventory[index]) {
                    game_import_t.cprintf(ent, Defines.PRINT_HIGH,
                            "No cells for power armor.\n");
                    return;
                }
                ent.flags |= Defines.FL_POWER_ARMOR;
                game_import_t
                        .sound(ent, Defines.CHAN_AUTO, game_import_t
                                .soundindex("misc/power1.wav"), 1,
                                Defines.ATTN_NORM, 0);
            }
        }
    };
    public static final EntUseAdapter Use_Item = new EntUseAdapter() {
        @Override
        public String getID() { return "use_item";}
        @Override
        public void use(edict_t ent, edict_t other, edict_t activator) {
            ent.svflags &= ~Defines.SVF_NOCLIENT;
            ent.use = null;
    
            if ((ent.spawnflags & Defines.ITEM_NO_TOUCH) != 0) {
                ent.solid = Defines.SOLID_BBOX;
                ent.touch = null;
            } else {
                ent.solid = Defines.SOLID_TRIGGER;
                ent.touch = Touch_Item;
            }
    
            game_import_t.linkentity(ent);
        }
    };

    /*
     * =============== GetItemByIndex ===============
     */
    public static gitem_t GetItemByIndex(int index) {
        if (index == 0 || index >= GameBase.game.num_items)
            return null;
    
        return GameItemList.itemlist[index];
    }

    /*
     * =============== FindItemByClassname
     * 
     * ===============
     */
    static gitem_t FindItemByClassname(String classname) {

        return Arrays.stream(GameItemList.itemlist, 1, GameBase.game.num_items).filter(it -> it.classname != null).filter(it -> it.classname.equalsIgnoreCase(classname)).findFirst().orElse(null);

    }

    /*
     * =============== FindItem ===============
     */
    
    static gitem_t FindItem(String pickup_name) {
        for (int i = 1; i < GameBase.game.num_items; i++) {
            gitem_t it = GameItemList.itemlist[i];
    
            if (it.pickup_name == null)
                continue;
            if (it.pickup_name.equalsIgnoreCase(pickup_name))
                return it;
        }
        Com.Println("Item not found:" + pickup_name);
        return null;
    }

    static void SetRespawn(edict_t ent, float delay) {
        ent.flags |= Defines.FL_RESPAWN;
        ent.svflags |= Defines.SVF_NOCLIENT;
        ent.solid = Defines.SOLID_NOT;
        ent.nextthink = GameBase.level.time + delay;
        ent.think = DoRespawn;
        game_import_t.linkentity(ent);
    }

    static int ITEM_INDEX(gitem_t item) {
        return item.index;
    }

    static edict_t Drop_Item(edict_t ent, gitem_t item) {
        edict_t dropped;
        float[] forward = { 0, 0, 0 };
        float[] right = { 0, 0, 0 };
        float[] offset = { 0, 0, 0 };
    
        dropped = GameUtil.G_Spawn();
    
        dropped.classname = item.classname;
        dropped.item = item;
        dropped.spawnflags = Defines.DROPPED_ITEM;
        dropped.s.effects = item.world_model_flags;
        dropped.s.renderfx = Defines.RF_GLOW;
        Math3D.VectorSet(dropped.mins, -15, -15, -15);
        Math3D.VectorSet(dropped.maxs, 15, 15, 15);
        game_import_t.setmodel(dropped, dropped.item.world_model);
        dropped.solid = Defines.SOLID_TRIGGER;
        dropped.movetype = Defines.MOVETYPE_TOSS;
    
        dropped.touch = drop_temp_touch;
    
        dropped.owner = ent;
    
        if (ent.client != null) {
            trace_t trace;
    
            Math3D.AngleVectors(ent.client.v_angle, forward, right, null);
            Math3D.VectorSet(offset, 24, 0, -16);
            Math3D.G_ProjectSource(ent.s.origin, offset, forward, right,
                    dropped.s.origin);
            trace = game_import_t.trace(ent.s.origin, dropped.mins, dropped.maxs,
                    dropped.s.origin, ent, Defines.CONTENTS_SOLID);
            Math3D.VectorCopy(trace.endpos, dropped.s.origin);
        } else {
            Math3D.AngleVectors(ent.s.angles, forward, right, null);
            Math3D.VectorCopy(ent.s.origin, dropped.s.origin);
        }
    
        Math3D.VectorScale(forward, 100, dropped.velocity);
        dropped.velocity[2] = 300;
    
        dropped.think = drop_make_touchable;
        dropped.nextthink = GameBase.level.time + 1;
    
        game_import_t.linkentity(dropped);
    
        return dropped;
    }

    static void Use_Item(edict_t ent, edict_t other, edict_t activator) {
        ent.svflags &= ~Defines.SVF_NOCLIENT;
        ent.use = null;
    
        if ((ent.spawnflags & Defines.ITEM_NO_TOUCH) != 0) {
            ent.solid = Defines.SOLID_BBOX;
            ent.touch = null;
        } else {
            ent.solid = Defines.SOLID_TRIGGER;
            ent.touch = Touch_Item;
        }
    
        game_import_t.linkentity(ent);
    }

    static int PowerArmorType(edict_t ent) {
        if (ent.client == null)
            return Defines.POWER_ARMOR_NONE;
    
        if (0 == (ent.flags & Defines.FL_POWER_ARMOR))
            return Defines.POWER_ARMOR_NONE;
    
        if (ent.client.pers.inventory[power_shield_index] > 0)
            return Defines.POWER_ARMOR_SHIELD;
    
        if (ent.client.pers.inventory[power_screen_index] > 0)
            return Defines.POWER_ARMOR_SCREEN;
    
        return Defines.POWER_ARMOR_NONE;
    }

    static int ArmorIndex(edict_t ent) {
        if (ent.client == null)
            return 0;
    
        if (ent.client.pers.inventory[jacket_armor_index] > 0)
            return jacket_armor_index;
    
        if (ent.client.pers.inventory[combat_armor_index] > 0)
            return combat_armor_index;
    
        if (ent.client.pers.inventory[body_armor_index] > 0)
            return body_armor_index;
    
        return 0;
    }

    public static boolean Pickup_PowerArmor(edict_t ent, edict_t other) {
        int quantity;
    
        quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
        other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
        if (GameBase.deathmatch.value != 0) {
            if (0 == (ent.spawnflags & Defines.DROPPED_ITEM))
                SetRespawn(ent, ent.item.quantity);
            
            if (0 == quantity)
                ent.item.use.use(other, ent.item);
        }
    
        return true;
    }

    public static boolean Add_Ammo(edict_t ent, gitem_t item, int count) {
        int index;
        int max;
    
        if (null == ent.client)
            return false;
    
        if (item.tag == Defines.AMMO_BULLETS)
            max = ent.client.pers.max_bullets;
        else if (item.tag == Defines.AMMO_SHELLS)
            max = ent.client.pers.max_shells;
        else if (item.tag == Defines.AMMO_ROCKETS)
            max = ent.client.pers.max_rockets;
        else if (item.tag == Defines.AMMO_GRENADES)
            max = ent.client.pers.max_grenades;
        else if (item.tag == Defines.AMMO_CELLS)
            max = ent.client.pers.max_cells;
        else if (item.tag == Defines.AMMO_SLUGS)
            max = ent.client.pers.max_slugs;
        else
            return false;
    
        index = ITEM_INDEX(item);
    
        if (ent.client.pers.inventory[index] == max)
            return false;
    
        ent.client.pers.inventory[index] += count;
    
        if (ent.client.pers.inventory[index] > max)
            ent.client.pers.inventory[index] = max;
    
        return true;
    }

    public static void InitItems() {
        GameBase.game.num_items = GameItemList.itemlist.length - 1;
    }

    /*
     * =============== SetItemNames
     * 
     * Called by worldspawn ===============
     */
    public static void SetItemNames() {
        int i;
        gitem_t it;
    
        for (i = 1; i < GameBase.game.num_items; i++) {
            it = GameItemList.itemlist[i];
            game_import_t.configstring(Defines.CS_ITEMS + i, it.pickup_name);
        }
    
        jacket_armor_index = ITEM_INDEX(FindItem("Jacket Armor"));
        combat_armor_index = ITEM_INDEX(FindItem("Combat Armor"));
        body_armor_index = ITEM_INDEX(FindItem("Body Armor"));
        power_screen_index = ITEM_INDEX(FindItem("Power Screen"));
        power_shield_index = ITEM_INDEX(FindItem("Power Shield"));
    }

    public static void SelectNextItem(edict_t ent, int itflags) {
        gclient_t cl;
        int i, index;
        gitem_t it;
    
        cl = ent.client;
    
        if (cl.chase_target != null) {
            GameChase.ChaseNext(ent);
            return;
        }
    
        
        for (i = 1; i <= Defines.MAX_ITEMS; i++) {
            index = (cl.pers.selected_item + i) % Defines.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (it.use == null)
                continue;
            if (0 == (it.flags & itflags))
                continue;
    
            cl.pers.selected_item = index;
            return;
        }
    
        cl.pers.selected_item = -1;
    }

    public static void SelectPrevItem(edict_t ent, int itflags) {
        gclient_t cl;
        int i, index;
        gitem_t it;
    
        cl = ent.client;
    
        if (cl.chase_target != null) {
            GameChase.ChasePrev(ent);
            return;
        }
    
        
        for (i = 1; i <= Defines.MAX_ITEMS; i++) {
            index = (cl.pers.selected_item + Defines.MAX_ITEMS - i)
                    % Defines.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (null == it.use)
                continue;
            if (0 == (it.flags & itflags))
                continue;
    
            cl.pers.selected_item = index;
            return;
        }
    
        cl.pers.selected_item = -1;
    }

    /*
     * =============== PrecacheItem
     * 
     * Precaches all data needed for a given item. This will be called for each
     * item spawned in a level, and for each item in each client's inventory.
     * ===============
     */
    public static void PrecacheItem(gitem_t it) {
        String s;
        String data;
        int len;
        gitem_t ammo;
    
        if (it == null)
            return;
    
        if (it.pickup_sound != null)
            game_import_t.soundindex(it.pickup_sound);
    
        if (it.world_model != null)
            game_import_t.modelindex(it.world_model);
    
        if (it.view_model != null)
            game_import_t.modelindex(it.view_model);
    
        if (it.icon != null)
            game_import_t.imageindex(it.icon);
    
        
        if (it.ammo != null && it.ammo.length() != 0) {
            ammo = FindItem(it.ammo);
            if (ammo != it)
                PrecacheItem(ammo);
        }
    
        
        s = it.precaches;
        if (s == null || s.length() != 0)
            return;
    
        StringTokenizer tk = new StringTokenizer(s);
    
        while (tk.hasMoreTokens()) {
            data = tk.nextToken();
    
            len = data.length();
    
            if (len >= Defines.MAX_QPATH || len < 5)
                game_import_t
                        .error("PrecacheItem: it.classname has bad precache string: "
                                + s);
    
            
            if (data.endsWith("md2"))
                game_import_t.modelindex(data);
            else if (data.endsWith("sp2"))
                game_import_t.modelindex(data);
            else if (data.endsWith("wav"))
                game_import_t.soundindex(data);
            else if (data.endsWith("pcx"))
                game_import_t.imageindex(data);
            else
                game_import_t.error("PrecacheItem: bad precache string: " + data);
        }
    }

    /*
     * ============ SpawnItem
     * 
     * Sets the clipping size and plants the object on the floor.
     * 
     * Items can't be immediately dropped to floor, because they might be on an
     * entity that hasn't spawned yet. ============
     */
    public static void SpawnItem(edict_t ent, gitem_t item) {
        PrecacheItem(item);
    
        if (ent.spawnflags != 0) {
            if (Lib.strcmp(ent.classname, "key_power_cube") != 0) {
                ent.spawnflags = 0;
                game_import_t.dprintf(ent.classname + " at " + Lib.vtos(ent.s.origin) + " has invalid spawnflags setAt\n");
            }
        }
    
        
        if (GameBase.deathmatch.value != 0) {
            if (((int) GameBase.dmflags.value & Defines.DF_NO_ARMOR) != 0) {
                if (item.pickup == Pickup_Armor
                        || item.pickup == Pickup_PowerArmor) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Defines.DF_NO_ITEMS) != 0) {
                if (item.pickup == Pickup_Powerup) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Defines.DF_NO_HEALTH) != 0) {
                if (Stream.of(Pickup_Health, Pickup_Adrenaline, Pickup_AncientHead).anyMatch(entInteractAdapter -> item.pickup == entInteractAdapter)) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Defines.DF_INFINITE_AMMO) != 0) {
                if ((item.flags == Defines.IT_AMMO)
                        || (Lib.strcmp(ent.classname, "weapon_bfg") == 0)) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
        }
    
        if (GameBase.coop.value != 0
                && (Lib.strcmp(ent.classname, "key_power_cube") == 0)) {
            ent.spawnflags |= (1 << (8 + GameBase.level.power_cubes));
            GameBase.level.power_cubes++;
        }
    
        
        if ((GameBase.coop.value != 0)
                && (item.flags & Defines.IT_STAY_COOP) != 0) {
            item.drop = null;
        }
    
        ent.item = item;
        ent.nextthink = GameBase.level.time + 2 * Defines.FRAMETIME;
        
        ent.think = droptofloor;
        ent.s.effects = item.world_model_flags;
        ent.s.renderfx = Defines.RF_GLOW;
    
        if (ent.model != null)
            game_import_t.modelindex(ent.model);
    }

    /*
     * QUAKED item_health (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    public static void SP_item_health(edict_t self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Defines.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
        }
    
        self.model = "models/items/healing/medium/tris.md2";
        self.count = 10;
        SpawnItem(self, FindItem("Health"));
        game_import_t.soundindex("items/n_health.wav");
    }

    /*
     * QUAKED item_health_small (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_small(edict_t self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Defines.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/healing/stimpack/tris.md2";
        self.count = 2;
        SpawnItem(self, FindItem("Health"));
        self.style = Defines.HEALTH_IGNORE_MAX;
        game_import_t.soundindex("items/s_health.wav");
    }

    /*
     * QUAKED item_health_large (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_large(edict_t self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Defines.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/healing/large/tris.md2";
        self.count = 25;
        SpawnItem(self, FindItem("Health"));
        game_import_t.soundindex("items/l_health.wav");
    }

    /*
     * QUAKED item_health_mega (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_mega(edict_t self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Defines.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/mega_h/tris.md2";
        self.count = 100;
        SpawnItem(self, FindItem("Health"));
        game_import_t.soundindex("items/m_health.wav");
        self.style = Defines.HEALTH_IGNORE_MAX | Defines.HEALTH_TIMED;
    }

    /*
     * =============== 
     * Touch_Item 
     * ===============
     */
    public static void Touch_Item(edict_t ent, edict_t other, cplane_t plane,
            csurface_t surf) {
        boolean taken;

        
        if (other.client == null || ent.item == null)
            return;
        if (other.health < 1)
            return; 
        if (ent.item.pickup == null)
            return; 
    
        taken = ent.item.pickup.interact(ent, other);
    
        if (taken) {
            
            other.client.bonus_alpha = 0.25f;
    
            
            other.client.ps.stats[Defines.STAT_PICKUP_ICON] = (short) game_import_t
                    .imageindex(ent.item.icon);
            other.client.ps.stats[Defines.STAT_PICKUP_STRING] = (short) (Defines.CS_ITEMS + ITEM_INDEX(ent.item));
            other.client.pickup_msg_time = GameBase.level.time + 3.0f;
    
            
            if (ent.item.use != null)
                other.client.pers.selected_item = other.client.ps.stats[Defines.STAT_SELECTED_ITEM] = (short) ITEM_INDEX(ent.item);
    
            if (ent.item.pickup == Pickup_Health) {
                if (ent.count == 2)
                    game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                            .soundindex("items/s_health.wav"), 1,
                            Defines.ATTN_NORM, 0);
                else if (ent.count == 10)
                    game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                            .soundindex("items/n_health.wav"), 1,
                            Defines.ATTN_NORM, 0);
                else if (ent.count == 25)
                    game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                            .soundindex("items/l_health.wav"), 1,
                            Defines.ATTN_NORM, 0);
                else
                    
                    game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                            .soundindex("items/m_health.wav"), 1,
                            Defines.ATTN_NORM, 0);
            } else if (ent.item.pickup_sound != null) {
                game_import_t.sound(other, Defines.CHAN_ITEM, game_import_t
                        .soundindex(ent.item.pickup_sound), 1,
                        Defines.ATTN_NORM, 0);
            }
        }
    
        if (0 == (ent.spawnflags & Defines.ITEM_TARGETS_USED)) {
            GameUtil.G_UseTargets(ent, other);
            ent.spawnflags |= Defines.ITEM_TARGETS_USED;
        }
    
        if (!taken)
            return;
    
        if (!((GameBase.coop.value != 0) && (ent.item.flags & Defines.IT_STAY_COOP) != 0)
                || 0 != (ent.spawnflags & (Defines.DROPPED_ITEM | Defines.DROPPED_PLAYER_ITEM))) {
            if ((ent.flags & Defines.FL_RESPAWN) != 0)
                ent.flags &= ~Defines.FL_RESPAWN;
            else
                GameUtil.G_FreeEdict(ent);
        }
    }

}
