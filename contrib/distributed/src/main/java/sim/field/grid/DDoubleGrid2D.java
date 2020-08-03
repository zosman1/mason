package sim.field.grid;

import java.rmi.RemoteException;

import sim.engine.DSimState;
import sim.field.DAbstractGrid2D;
import sim.field.HaloGrid2D;
import sim.field.partitioning.PartitionInterface;
import sim.field.storage.DoubleGridStorage;
import sim.util.*;

public class DDoubleGrid2D extends DAbstractGrid2D {

	private HaloGrid2D<Double, NumberND, DoubleGridStorage<Double>> halo;
	public final double initVal;

	public DDoubleGrid2D(final PartitionInterface ps, final int[] aoi, final double initVal, final DSimState state) {
		super(ps);
		if (ps.getNumDim() != 2)
			throw new IllegalArgumentException("The number of dimensions is expected to be 2, got: " + ps.getNumDim());

		halo = new HaloGrid2D<Double, NumberND, DoubleGridStorage<Double>>(ps, aoi,
				new DoubleGridStorage(ps.getPartition(), initVal), state);

		this.initVal = initVal;
	}

	public double[] getStorageArray() {
		return (double[]) halo.localStorage.getStorage();
	}

	public double getLocal(final Int2D p) {
		return getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))];
	}

	public void addLocal(final Int2D p, final double t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public Double getRMI(final Int2D p) {
		return getLocal(p);
	}

	public void addLocal(final Int2D p, final Double t) {
		getStorageArray()[halo.localStorage.getFlatIdx(halo.toLocalPoint(p))] = t;
	}

	public void removeLocal(final Int2D p, final Double t) {
		removeLocal(p);
	}

	public void removeLocal(final Int2D p) {
		addLocal(p, initVal);
	}

	public double get(final Int2D p) {
		if (!halo.inLocalAndHalo(p)) {
			System.out.println(String.format("PID %d get %s is out of local boundary, accessing remotely through RMI",
					halo.partition.getPid(), p.toString()));
			// TODO: Should this be (Double)?
			return (double) halo.getFromRemote(p);
		} else
			return getLocal(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void add(final Int2D p, final Double val) {
		if (!halo.inLocal(p))
			halo.addToRemote(p, val);
		else
			addLocal(p, val);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void remove(final Int2D p, final Double t) {
		halo.remove(p);
	}

	// Overloading to prevent AutoBoxing-UnBoxing
	public void move(final Int2D fromP, final Int2D toP, final double t) {
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

	public DDoubleGrid2D multiply(double byThisMuch) {
		if (byThisMuch == 1.0)
			return this;

		for (Int2D p : halo.partition.getPartition()) {
			Double obj = get(p);
			removeLocal(p);
			add(p, obj * byThisMuch);
		}
		return this;
	}

}
