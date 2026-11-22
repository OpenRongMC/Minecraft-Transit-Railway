package org.mtr.mod.entity;

import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

public record TransportPose(Vector position, double yaw, double pitch) {

	public static TransportPose forCar(VehicleCar car, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>> bogies, boolean hasPitch) {
		if (bogies.isEmpty() || bogies.size() > 2) {
			return null;
		}

		final Vector first = bogies.get(0).left();
		final Vector second = bogies.get(0).right();
		final Vector front = bogies.size() == 1 ? first : Vector.getAverage(first, second);
		final Vector back = bogies.size() == 1 ? second : Vector.getAverage(bogies.get(1).left(), bogies.get(1).right());
		final double dx = back.x - front.x;
		final double dz = back.z - front.z;
		final double yaw = Math.atan2(dx, dz);
		final double pitch = hasPitch ? Math.atan2(back.y - front.y, Math.hypot(dx, dz)) : 0;
		final double pivot = -(car.getBogie1Position() + car.getBogie2Position()) / 2;
		final Vector position = Vector.getAverage(front, back).add(new Vector(0, 0, pivot).rotateX(pitch).rotateY(yaw));
		return new TransportPose(position, yaw, pitch);
	}
}
