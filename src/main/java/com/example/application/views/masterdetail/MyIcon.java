package com.example.application.views.masterdetail;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

abstract class MyIcon extends Icon {

    public MyIcon(VaadinIcon icon) {
        super(icon);
        addClassNames(LumoUtility.IconSize.SMALL);
    }
}
