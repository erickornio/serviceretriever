package net.svcret.admin.client.ui.config.auth;

import static net.svcret.admin.client.AdminPortal.*;
import net.svcret.admin.shared.IAsyncLoadCallback;
import net.svcret.admin.shared.Model;
import net.svcret.admin.shared.model.BaseGAuthHost;
import net.svcret.admin.shared.model.GAuthenticationHostList;
import net.svcret.admin.shared.model.GUser;

public class AddUserPanel extends BaseUserPanel {

	public AddUserPanel(final long theAuthHostPid) {
		super();

		Model.getInstance().loadAuthenticationHosts(new IAsyncLoadCallback<GAuthenticationHostList>() {
			@Override
			public void onSuccess(GAuthenticationHostList theResult) {
				BaseGAuthHost authHost = theResult.getAuthHostByPid(theAuthHostPid);
				initContents();
				setUser(new GUser(), authHost);
			}
		});

	}


	@Override
	protected String getPanelTitle() {
		return MSGS.editUser_Title();
	}

}
