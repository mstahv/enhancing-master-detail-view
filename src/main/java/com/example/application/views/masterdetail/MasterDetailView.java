package com.example.application.views.masterdetail;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.SamplePersonService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

@PageTitle("Master-Detail")
@Route(value = "master-detail/:samplePersonID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
public class MasterDetailView extends SplitLayout implements BeforeEnterObserver {

    private final String SAMPLEPERSON_ID = "samplePersonID";
    private final String SAMPLEPERSON_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

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

    public MasterDetailView(SamplePersonService samplePersonService) {
        this.samplePersonService = samplePersonService;
        setOrientation(Orientation.HORIZONTAL);
        setSizeFull();
        addToPrimary(grid);
        addToSecondary(createEditorLayout());

        // Configure Grid
        grid.addColumn("firstName").setAutoWidth(true);
        grid.addColumn("lastName").setAutoWidth(true);
        grid.addColumn("email").setAutoWidth(true);
        grid.addColumn("phone").setAutoWidth(true);
        grid.addColumn("dateOfBirth").setAutoWidth(true);
        grid.addColumn("occupation").setAutoWidth(true);
        grid.addColumn("role").setAutoWidth(true);

        grid.addComponentColumn(p ->
            p.isImportant() ? new CheckedIcon() : new UncheckedIcon())
                .setHeader("Important").setAutoWidth(true);

        grid.setItems(query -> samplePersonService.stream(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                );
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SAMPLEPERSON_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                prepareFormForNewPerson();
                UI.getCurrent().navigate(MasterDetailView.class);
            }
        });
        grid.setSizeFull();

        // Configure Form
        binder = new BeanValidationBinder<>(SamplePerson.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);
        prepareFormForNewPerson();

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
                    UI.getCurrent().navigate(MasterDetailView.class);
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
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> samplePersonId = event.getRouteParameters().get(SAMPLEPERSON_ID).map(Long::parseLong);
        if (samplePersonId.isPresent()) {
            Optional<SamplePerson> samplePersonFromBackend = samplePersonService.get(samplePersonId.get());
            if (samplePersonFromBackend.isPresent()) {
                binder.setBean(samplePersonFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested samplePerson was not found, ID = %s", samplePersonId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(MasterDetailView.class);
            }
        }
    }

    private Component createEditorLayout() {
        var formLayout = new FormLayout();
        formLayout.setClassName("editor");
        formLayout.add(firstName, lastName, email, phone, dateOfBirth, occupation, role, important);

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
        binder.setBean(new SamplePerson());
    }

}
