package com.example.application.views.masterdetail;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class UncheckedIcon extends MyIcon {

    public UncheckedIcon() {
        super(VaadinIcon.MINUS);
        addClassNames(LumoUtility.TextColor.DISABLED);
    }
}
