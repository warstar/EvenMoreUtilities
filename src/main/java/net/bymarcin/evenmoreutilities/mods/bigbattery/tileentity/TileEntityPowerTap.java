package net.bymarcin.evenmoreutilities.mods.bigbattery.tileentity;

import java.util.HashSet;
import java.util.Set;

import net.bymarcin.evenmoreutilities.mods.bigbattery.BigBattery;
import net.bymarcin.evenmoreutilities.mods.bigbattery.gui.PowerTapContener;
import net.bymarcin.evenmoreutilities.mods.bigbattery.gui.PowerTapUpdatePacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import cofh.api.energy.IEnergyHandler;
import cofh.util.BlockHelper;
import cofh.util.EnergyHelper;
import cofh.util.ServerHelper;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import erogenousbeef.core.multiblock.MultiblockControllerBase;
import erogenousbeef.core.multiblock.MultiblockValidationException;
import erogenousbeef.core.multiblock.rectangular.RectangularMultiblockTileEntityBase;

public class TileEntityPowerTap extends RectangularMultiblockTileEntityBase implements IEnergyHandler{
	int transferMax = 0;
	int transferCurrent = 0;
	private Set<EntityPlayer> updatePlayers = new HashSet<EntityPlayer>();
	
	public void beginUpdatingPlayer(EntityPlayer playerToUpdate) {
		updatePlayers.add(playerToUpdate);
		sendIndividualUpdate(playerToUpdate);
	}	

	protected void sendIndividualUpdate(EntityPlayer player) {
		if(this.worldObj.isRemote) { return; }
		PacketDispatcher.sendPacketToPlayer(getUpdatePacket(), (Player)player);
	}
	
	protected Packet getUpdatePacket(){
	     return new PowerTapUpdatePacket(xCoord,yCoord,zCoord,transferCurrent,PowerTapUpdatePacket.UPDATE).makePacket();
	}
	
	public void stopUpdatingPlayer(EntityPlayer playerToRemove) {
		updatePlayers.remove(playerToRemove);
	}
	
	public int getTransferCurrent(){
		return transferCurrent;
	}
	
	@Override
	public void isGoodForFrame() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's frame", new Object[] { Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord) }));	
	}

	@Override
	public void isGoodForSides() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's sides", new Object[] { Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord) }));
	}

	@Override
	public void isGoodForTop() throws MultiblockValidationException {
		TileEntity entityBelow = this.worldObj.getBlockTileEntity(this.xCoord,  this.yCoord - 1, this.zCoord);
		if ((entityBelow instanceof TileEntityElectrode)) {
			return;
		}
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap must be placed on electrode", new Object[] { Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord) }));	
	}

	@Override
	public void isGoodForBottom() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's bottom", new Object[] { Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord) }));
	}

	@Override
	public void isGoodForInterior() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's interior", new Object[] { Integer.valueOf(this.xCoord), Integer.valueOf(this.yCoord), Integer.valueOf(this.zCoord) }));	
	}
	
	@Override
	public void onMachineAssembled(MultiblockControllerBase controller) {
		super.onMachineAssembled(controller);
		transferMax = ((BigBattery)getMultiblockController()).getStorage().getMaxReceive();
		if(transferCurrent > transferMax)
			transferCurrent=transferMax;
	}

	@Override
	public void onMachineActivated() {
	}

	@Override
	public void onMachineDeactivated() {
	}
	
	public int onTransferEnergy(){
			if(ServerHelper.isClientWorld(worldObj) || isOutput() || getMultiblockController()==null) return 0;
			TileEntity tile = BlockHelper.getAdjacentTileEntity(this, ForgeDirection.UP);
			int energyGet=0;
			if (EnergyHelper.isEnergyHandlerFromSide(tile,ForgeDirection.VALID_DIRECTIONS[(1 ^ 0x1)])){
				energyGet = ((IEnergyHandler)tile).receiveEnergy(ForgeDirection.VALID_DIRECTIONS[(1 ^ 0x1)], Math.min(transferCurrent, ((BigBattery)getMultiblockController()).getStorage().getEnergyStored()), false); 
			}  
			((BigBattery)getMultiblockController()).getStorage().modifyEnergyStored(-energyGet);
			return energyGet;
	}

	public void setTransfer(int transfer){
		transferCurrent = Math.max(0,Math.min(transfer, transferMax));	
	}
	
	public void onPacket(int transfer,int typ){
		switch(typ){
			case PowerTapUpdatePacket.MINUS: 
			case PowerTapUpdatePacket.PLUS:setTransfer(transfer); updatePowerTap(); break;
			case PowerTapUpdatePacket.UPDATE: setTransfer(transfer); updatePowerTap(); break;
		}
	}
	
	public void updatePowerTap(){
		for(EntityPlayer p: updatePlayers)
			PacketDispatcher.sendPacketToPlayer(new PowerTapUpdatePacket(xCoord, yCoord, zCoord, transferCurrent, PowerTapUpdatePacket.UPDATE).makePacket(), (Player) p);
	}
	
	public Container getContainer(EntityPlayer player){
		updatePowerTap();
		return new PowerTapContener(this, player);
	}
	
	
	@Override
	public MultiblockControllerBase createNewMultiblock() {
		return new BigBattery(this.worldObj);
	}

	@Override
	public Class<? extends MultiblockControllerBase> getMultiblockControllerType() {
		return BigBattery.class;
	}
	
	/* IEnergyHandler */
	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		if(getMultiblockController()!=null && isOutput() && getMultiblockController().isAssembled()){
			 int temp =((BigBattery)getMultiblockController()).getStorage().receiveEnergy(Math.min(maxReceive,transferCurrent), simulate);
			 ((BigBattery)getMultiblockController()).modifyLastTickBalance(temp);
			 return temp;
		}
		return 0;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		if(getMultiblockController()!=null && !isOutput() && getMultiblockController().isAssembled()){
			int temp = ((BigBattery)getMultiblockController()).getStorage().extractEnergy(Math.min(maxExtract,transferCurrent), simulate);
			 ((BigBattery)getMultiblockController()).modifyLastTickBalance(-temp);
			return temp;
		}
		return 0;
	}

	@Override
	public boolean canInterface(ForgeDirection from) {
		if(from == ForgeDirection.UP)
			return true;
		return false;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		if(getMultiblockController()!=null && from == ForgeDirection.UP){
			return ((BigBattery)getMultiblockController()).getStorage().getEnergyStored();
		}
		return 0;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		if(getMultiblockController()!=null && from == ForgeDirection.UP){
			return ((BigBattery)getMultiblockController()).getStorage().getMaxEnergyStored();
		}
		return 0;
	}
	
	public boolean isOutput() {
		return worldObj.getBlockMetadata(xCoord, yCoord, zCoord)==0;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		transferCurrent = data.getInteger("transfer");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setInteger("transfer",transferCurrent);
	}
	
	public void setIn(){
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2);
	}
	
	public void setOut(){
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 2);
	}
}
