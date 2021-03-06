package net.bymarcin.evenmoreutilities.mods.bigbattery.block;

import net.bymarcin.evenmoreutilities.EvenMoreUtilities;
import net.bymarcin.evenmoreutilities.mods.bigbattery.BigBatteryMod;
import net.bymarcin.evenmoreutilities.mods.bigbattery.tileentity.TileEntityWall;
import net.bymarcin.evenmoreutilities.utils.StaticValues;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import erogenousbeef.core.multiblock.BlockMultiblockBase;
import erogenousbeef.core.multiblock.IMultiblockPart;
import erogenousbeef.core.multiblock.MultiblockControllerBase;

public class BlockBigBatteryWall extends BlockMultiblockBase {
	public static BlockBigBatteryWall instance = new BlockBigBatteryWall(BigBatteryMod.blockBigBatteryWallID);
	public static Icon icon;
	public static Icon corner;
	public static Icon horizontal;
	public static Icon vertical;
	public static Icon center;

	  public static final int CASING_METADATA_BASE = 0;
	  public static final int CASING_CORNER = 1;
	  public static final int CASING_CENTER = 2;
	  public static final int CASING_VERTICAL = 3;
	  public static final int CASING_EASTWEST = 4;
	  public static final int CASING_NORTHSOUTH = 5;
	

	public BlockBigBatteryWall(int id) {
		super(id, Material.iron);
		this.setCreativeTab(EvenMoreUtilities.instance.tabCustom);
		this.setHardness(3.0F);
		this.setUnlocalizedName("emu.bigBatteryWall");
	}

	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityWall();
	}

	@Override
	public Icon getIcon(int par1, int par2) {
		switch(par2){
		case BlockBigBatteryWall.CASING_METADATA_BASE: return icon;
		case BlockBigBatteryWall.CASING_CORNER: return corner;
		case BlockBigBatteryWall.CASING_CENTER: return center;
		case BlockBigBatteryWall.CASING_EASTWEST: return horizontal;
		case BlockBigBatteryWall.CASING_NORTHSOUTH: return (par1 == 0 || par1 == 1)?vertical:horizontal;
		case BlockBigBatteryWall.CASING_VERTICAL: return vertical;
		}
		
		return icon;
	}

	@Override
	public void registerIcons(IconRegister iconRegister) {
		icon = iconRegister.registerIcon(StaticValues.modId + ":bb_part");
		corner = iconRegister.registerIcon(StaticValues.modId + ":bb_corner");
		horizontal = iconRegister.registerIcon(StaticValues.modId + ":bb_horizontal");
		vertical = iconRegister.registerIcon(StaticValues.modId + ":bb_vertical");
		center = iconRegister.registerIcon(StaticValues.modId + ":bb_center");
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {	
		if(player.isSneaking()) {
			return false;
		}
		
		if(!world.isRemote) {
			ItemStack currentEquippedItem = player.getCurrentEquippedItem();
			if(currentEquippedItem == null) {
				TileEntity te = world.getBlockTileEntity(x, y, z);
				if(te instanceof IMultiblockPart) {
					MultiblockControllerBase controller = ((IMultiblockPart)te).getMultiblockController();
					if(controller != null) {
						Exception e = controller.getLastValidationException();
						if(e != null) {
							player.sendChatToPlayer(ChatMessageComponent.createFromText(e.getMessage()));
							return true;
						}
					}else {
						player.sendChatToPlayer(ChatMessageComponent.createFromText("Block is not connected to a battery. This could be due to lag, or a bug. If the problem persists, try breaking and re-placing the block."));
						return true;
					}
				}
			 }
		}
		return false;
	}
}
