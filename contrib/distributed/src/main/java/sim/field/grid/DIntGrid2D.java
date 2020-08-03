package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DAbstractGrid2D;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.IntGridStorage;
import sim.util.*;

public class DIntGrid2D extends DAbstractGrid2D {

	private HaloGrid2D<Integer, NumberND, IntGridStorage<Integer>> halo;
	public final int initVal;

	public DIntGrid2D(final PartitionInterface ps, final int[] aoi, final int initVal, final DSimState state) {
		super(ps);
		if (ps.getNumDim() != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + ps.getNumDim());

		halo = new HaloGrid2D<Integer, NumberND, IntGridStorage<Integer>>(ps, aoi,
				new IntGridStorage(ps.getPartition(), initVal), state);
		fieldSize = ps.getFieldSize();

		this.initVal = initVal;
	}

	public int[] getStorageArray() {
		return (int[]) halo.localStorage.getStorage();
	}

	public int getLocal(final Int2D p) {
		return getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))];
	}

	public void addLocal(final Int2D p, final int t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public Integer getRMI(final Int2D p) {
		return getLocal(p);
	}

	public void addLocal(final Int2D p, final Integer t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public void removeLocal(final Int2D p, final Double t) {
		removeLocal(p);
	}

	public void removeLocal(final Int2D p) {
		addLocal(p, initVal);
	}

	public int get(final Int2D p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			// TODO: Should this be (Double)?
			return (int) halo.getFromRemote(p);
		} else
			return getLocal(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void add(final Int2D p, final Integer val) {
		if (!halo.inLocal(p))
			halo.addToRemote(p, val);
		else
			addLocal(p, val);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void remove(final Int2D p, final Integer t) {
		halo.remove(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void move(final Int2D fromP, final Int2D toP, final Integer t) {
		final int fromPid = halo.partition.toPartitionId(fromP);
		final int toPid = halo.partition.toPartitionId(fromP);

		if (fromPid == toPid && fromPid != halo.partition.pid) {
			// So that we make only a single RMI call instead of two
			try {
				halo.proxy.getField(halo.partition.toPartitionId(fromP)).moveRMI(fromP, toP, t);
			} catch (final RemoteException e) {
				throw new RuntimeException(e);
			}
		} else {
			remove(fromP, t);
			add(toP, t);
		}
	}

}
