package com.example.application.views.masterdetail;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.SamplePersonService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

@PageTitle("Master-Detail")
@Route(value = "master-detail/:"+ MasterDetailView.SAMPLEPERSON_ID + "?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = ":"+ MasterDetailView.SAMPLEPERSON_ID+"?/:action?(edit)", layout = MainLayout.class)
@Uses(Icon.class)
public class MasterDetailView extends SplitLayout implements BeforeEnterObserver {

    public static final String SAMPLEPERSON_ID = "samplePersonID";
    private static final String SAMPLEPERSON_EDIT_ROUTE_TEMPLATE = "/%s/edit";

    private final Grid<SamplePerson> grid = new Grid<>(SamplePerson.class, false);

    private TextField firstName = new TextField("First Name");
    private TextField lastName = new TextField("Last Name");
    private TextField email = new TextField("Email");
    private TextField phone = new TextField("Phone");
    private DatePicker dateOfBirth = new DatePicker("Date Of Birth");
    private TextField occupation = new TextField("Occupation");
    private TextField role = new TextField("Role");
    private Checkbox important = new Checkbox("Important");;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<SamplePerson> binder;

    private final SamplePersonService samplePersonService;
    private boolean formHasChanges;

    public MasterDetailView(SamplePersonService samplePersonService) {
        this.samplePersonService = samplePersonService;
        setOrientation(Orientation.HORIZONTAL);
        setSizeFull();
        addToPrimary(grid);
        addToSecondary(createEditorLayout());

        // Configure Grid
        grid.setColumns("firstName","lastName" ,"email", "phone", "dateOfBirth", "occupation", "role");
        grid.addComponentColumn(p ->
            p.isImportant() ? new CheckedIcon() : new UncheckedIcon())
                .setHeader("Important");
        grid.getColumns().forEach(c -> c.setAutoWidth(true));

        grid.setItems(query -> samplePersonService.stream(VaadinSpringDataHelpers.toSpringPageRequest(query)));
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editPerson(event.getValue());
            } else {
                prepareFormForNewPerson();
            }
        });
        grid.setSizeFull();

        // Configure Form
        binder = new BeanValidationBinder<>(SamplePerson.class);
        binder.bindInstanceFields(this);
        binder.addStatusChangeListener(e -> {
            adjustSaveButtonState();
        });
        binder.addValueChangeListener(e -> {
            if(e.isFromClient()) {
                formHasChanges = true;
                adjustSaveButtonState();
            }
        });

        cancel.addClickListener(e -> {
            prepareFormForNewPerson();
            refreshGrid();
        });

        save.addClickListener(e -> {
            if(binder.isValid()) {
                try {
                    samplePersonService.update(binder.getBean());
                    prepareFormForNewPerson();
                    refreshGrid();
                    Notification.show("Data updated");
                } catch (ObjectOptimisticLockingFailureException exception) {
                    Notification n = Notification.show(
                            "Error updating the data. Somebody else has updated the record while you were making changes.");
                    n.setPosition(Position.MIDDLE);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
        save.addClickShortcut(Key.ENTER);
    }

    /**
     * Updates deep linkin parameters
     */
    private void updateRouteParemeters() {
        if(isAttached()) {
            String deepLinkingUrl = RouteConfiguration.forSessionScope().getUrl(getClass());
            if(binder.getBean().getId() != null) {
                deepLinkingUrl = deepLinkingUrl + String.format(SAMPLEPERSON_EDIT_ROUTE_TEMPLATE, binder.getBean().getId().toString());
            }
            getUI().get().getPage().getHistory()
                    .replaceState(null, deepLinkingUrl);
        }
    }

    private void adjustSaveButtonState() {
        // only allow saving if we have valida data
        save.setEnabled(binder.isValid() && formHasChanges);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        /*
         * When entering the view, check if there is an
         * if an existing person should be selected for
         * editing
         */
        Optional<Long> samplePersonId = event.getRouteParameters().get(SAMPLEPERSON_ID).map(Long::parseLong);
        if (samplePersonId.isPresent()) {
            Optional<SamplePerson> samplePersonFromBackend = samplePersonService.get(samplePersonId.get());
            if (samplePersonFromBackend.isPresent()) {
                editPerson(samplePersonFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested samplePerson was not found, ID = %s", samplePersonId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                prepareFormForNewPerson();
            }
        } else {
            prepareFormForNewPerson();
        }
    }

    private void editPerson(SamplePerson samplePersonFromBackend) {
        binder.setBean(samplePersonFromBackend);
        formHasChanges = false;
        updateRouteParemeters();
    }

    private Component createEditorLayout() {
        var formLayout = new FormLayout();
        formLayout.setClassName("editor");
        formLayout.add(firstName, lastName, email, phone, dateOfBirth, occupation, role, important);

        // Validate fields while users type in
        // for example when email becomes valid,
        // the error disappears automatically
        formLayout.getChildren().forEach(c -> {
                if (c instanceof HasValueChangeMode) {
                    ((HasValueChangeMode) c).setValueChangeMode(ValueChangeMode.LAZY);
                }
            }
        );

        var editorLayout = new VerticalLayout();
        editorLayout.setWidth("400px");
        editorLayout.setPadding(false);
        editorLayout.setSpacing(false);
        editorLayout.addAndExpand(formLayout);
        editorLayout.add(createButtonLayout());
        return editorLayout;
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        return buttonLayout;
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void prepareFormForNewPerson() {
        editPerson(new SamplePerson());
    }

}
