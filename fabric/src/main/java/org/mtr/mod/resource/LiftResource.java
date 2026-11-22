package org.mtr.mod.resource;

import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.serializer.WriterBase;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.generated.resource.LiftResourceSchema;

public final class LiftResource extends LiftResourceSchema {
	private final String objectModelId;

	public LiftResource(ReaderBase readerBase) {
		super(readerBase);
		objectModelId = readerBase.getString("objectModelId", "");
		updateData(readerBase);
	}

	/** Refers to an imported OBJ or Blockbench model from the same resource set. */
	public String getObjectModelId() {
		return objectModelId;
	}

	@Override
	public void serializeData(WriterBase writer) {
		super.serializeData(writer);
		if (!objectModelId.isEmpty()) writer.writeString("objectModelId", objectModelId);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return TextHelper.translatable(name).getString();
	}

	public int getColor() {
		return CustomResourceTools.colorStringToInt(color);
	}

	public Identifier getTexture() {
		return CustomResourceTools.formatIdentifierWithDefault(textureResource, "png");
	}
}
