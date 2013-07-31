package net.svcret.admin.client.ui.components;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

/**
 * A {@link Cell} used to render a button.
 */
public class PButtonCell extends ButtonCell {

	private ImageResource myIcon;

	/**
	 * Construct a new ButtonCell that will use a {@link SimpleSafeHtmlRenderer}.
	 */
	public PButtonCell(ImageResource theIcon) {
		super();

		myIcon = theIcon;
	}

	@Override
	public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("<button type=\"button\" class=\"" + CssConstants.PUSHBUTTON + "\" tabindex=\"-1\">");
		sb.appendHtmlConstant("<img src=\"" + myIcon.getSafeUri().asString() + "\"/>");
		sb.appendHtmlConstant("</button>");
	}

	@Override
	protected void onEnterKeyDown(Context context, Element parent, String value, NativeEvent event, ValueUpdater<String> valueUpdater) {
		if (valueUpdater != null) {
			valueUpdater.update(value);
		}
	}
}