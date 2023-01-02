package com.example.application.views.masterdetail;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class CheckedIcon extends MyIcon {

    public CheckedIcon() {
        super(VaadinIcon.CHECK);
        addClassNames(LumoUtility.TextColor.PRIMARY);
    }
}
