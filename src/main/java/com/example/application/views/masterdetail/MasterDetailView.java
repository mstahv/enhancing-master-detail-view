package com.example.application.views.masterdetail;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.SamplePersonService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
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

import java.util.Arrays;
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

    private final SamplePersonService service;

    // binder.hasChanges() methods does not
    // work in a meaningful way with buffered
    // binding, track changes with this field
    private boolean formHasChanges;

    public MasterDetailView(SamplePersonService samplePersonService) {
        this.service = samplePersonService;
        buildView();

        // Connect Grid to the backend
        listPersonsInGrid();

        // Configure form binding
        binder = new BeanValidationBinder<>(SamplePerson.class);
        binder.bindInstanceFields(this);

        configureEagerFormValidation();

        addListeners();

    }

    private void addListeners() {
        grid.asSingleSelect().addValueChangeListener(event -> {
            var selectedPerson = event.getValue();
            if(selectedPerson == null) {
                prepareFormForNewPerson();
            } else {
                editPerson(selectedPerson);
            }
        });

        cancel.addClickListener(e -> {
            prepareFormForNewPerson();
            listPersonsInGrid();
        });

        save.addClickListener(e -> {
            try {
                service.update(binder.getBean());
                prepareFormForNewPerson();
                listPersonsInGrid();
                notifyUser("Data updated");
            } catch (ObjectOptimisticLockingFailureException exception) {
                showErrorMessage("Error updating the data. Somebody else has updated the record while you were making changes.");
            }
        });
        save.addClickShortcut(Key.ENTER);
    }

    private void showErrorMessage(String errorMessage) {
        Notification n = Notification.show(errorMessage);
        n.setPosition(Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static void notifyUser(String Data_updated) {
        Notification.show(Data_updated);
    }

    private void buildView() {
        setOrientation(Orientation.HORIZONTAL);
        setSizeFull();

        var formLayout = new FormLayout(firstName, lastName, email, phone, dateOfBirth, occupation, role, important);
        formLayout.setClassName("editor");

        var buttonLayout = new HorizontalLayout(save, cancel);
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        var editorLayout = new VerticalLayout();
        editorLayout.setWidth("400px");
        editorLayout.setPadding(false);
        editorLayout.setSpacing(false);
        editorLayout.addAndExpand(formLayout);
        editorLayout.add(buttonLayout);

        addToSecondary(editorLayout);
        addToPrimary(grid);

        // Configure Grid
        grid.setColumns("firstName","lastName" ,"email", "phone", "dateOfBirth", "occupation", "role");
        grid.addComponentColumn(p ->
                        p.isImportant() ? new CheckedIcon() : new UncheckedIcon())
                .setHeader("Important")
                .setKey("important")
                .setSortable(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.getColumns().forEach(c -> {
            c.setAutoWidth(true);
            c.setVisible(false);
        });

        // Show only reasonable amount of columns by default:
        // Better performance and UX
        // Everybody looking at this code snippet, please go and
        // vote https://github.com/vaadin/flow-components/issues/1603
        Arrays.asList("firstName","lastName" ,"email", "important")
                .forEach(key -> grid.getColumnByKey(key).setVisible(true));
        grid.setSizeFull();

        // Use Grid's built in context menu to show/hide cols
        GridContextMenu<SamplePerson> columnSelector = grid.addContextMenu();
        columnSelector.add(new H6("Available columns:"));
        grid.getColumns().forEach(col -> {
            GridMenuItem<SamplePerson> item = columnSelector.addItem(col.getHeaderText());
            item.setCheckable(true);
            item.setChecked(col.isVisible());
            item.addMenuItemClickListener(e -> {
                col.setVisible(!col.isVisible());
                item.setChecked(col.isVisible());
            });
        });
        // add a tooltip for people to find the functionality
        Tooltip.forComponent(grid)
                .withText("Context click to edit visible columns");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        /*
         * When entering the view, check if there is an
         * if an existing person should be selected for
         * editing based on the current URL
         */
        Optional<Long> samplePersonId = event.getRouteParameters().get(SAMPLEPERSON_ID).map(Long::parseLong);
        if (samplePersonId.isPresent()) {
            Optional<SamplePerson> samplePersonFromBackend = service.get(samplePersonId.get());
            if (samplePersonFromBackend.isPresent()) {
                editPerson(samplePersonFromBackend.get());
            } else {
                showErrorMessage("The requested samplePerson was not found, ID = %s");
                prepareFormForNewPerson();
            }
        } else {
            prepareFormForNewPerson();
        }
    }

    /**
     * Updates deep linkin parameters.
     */
    private void updateRouteParameters() {
        if(isAttached()) {
            String deepLinkingUrl = RouteConfiguration.forSessionScope().getUrl(getClass());
            if(binder.getBean().getId() != null) {
                deepLinkingUrl = deepLinkingUrl + String.format(SAMPLEPERSON_EDIT_ROUTE_TEMPLATE, binder.getBean().getId().toString());
            }
            getUI().get().getPage().getHistory()
                    .replaceState(null, deepLinkingUrl);
        }
    }


    private void editPerson(SamplePerson person) {
        binder.setBean(person);
        formHasChanges = false;
        updateRouteParameters();
    }

    private void listPersonsInGrid() {
        grid.setItems(query -> service.stream(VaadinSpringDataHelpers.toSpringPageRequest(query)));
    }

    private void prepareFormForNewPerson() {
        editPerson(new SamplePerson());
    }

    private void configureEagerFormValidation() {
        // Validate fields while users type in
        // for example when email becomes valid,
        // the error disappears automatically
        binder.getFields().forEach(c -> {
            if (c instanceof HasValueChangeMode) {
                ((HasValueChangeMode) c).setValueChangeMode(ValueChangeMode.LAZY);
            }
        });
        binder.addStatusChangeListener(e -> {
            adjustSaveButtonState();
        });
        binder.addValueChangeListener(e -> {
            if(e.isFromClient()) {
                formHasChanges = true;
                adjustSaveButtonState();
            }
        });
    }

    private void adjustSaveButtonState() {
        // only allow saving if we have valida data
        save.setEnabled(binder.isValid() && formHasChanges);
    }

}
