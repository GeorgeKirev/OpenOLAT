/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.appointments.ui;

import static org.olat.core.gui.components.util.KeyValues.VALUE_ASC;
import static org.olat.core.gui.components.util.KeyValues.entry;
import static org.olat.core.util.ArrayHelper.emptyStrings;
import static org.olat.modules.appointments.ui.StartDuration.getEnd;
import static org.olat.modules.appointments.ui.StartDuration.none;
import static org.olat.modules.appointments.ui.StartDuration.ofString;
import static org.olat.modules.bigbluebutton.ui.BigBlueButtonUIHelper.getSelectedTemplate;
import static org.olat.modules.bigbluebutton.ui.BigBlueButtonUIHelper.isWebcamLayoutAvailable;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.util.KeyValues;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.translator.TranslatorHelper;
import org.olat.core.id.Identity;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.DateUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.modules.appointments.Appointment;
import org.olat.modules.appointments.AppointmentsService;
import org.olat.modules.appointments.Topic;
import org.olat.modules.appointments.TopicLight.Type;
import org.olat.modules.bigbluebutton.BigBlueButtonDispatcher;
import org.olat.modules.bigbluebutton.BigBlueButtonMeeting;
import org.olat.modules.bigbluebutton.BigBlueButtonMeetingLayoutEnum;
import org.olat.modules.bigbluebutton.BigBlueButtonMeetingTemplate;
import org.olat.modules.bigbluebutton.ui.BigBlueButtonMeetingsCalendarController;
import org.olat.modules.bigbluebutton.ui.BigBlueButtonUIHelper;
import org.olat.modules.bigbluebutton.ui.EditBigBlueButtonMeetingController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRelationType;
import org.olat.repository.RepositoryService;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 4 Jun 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class AppointmentCreateController extends FormBasicController {
	
	private static final String CMD_DURATION = "dur_";
	private static final String KEY_ON = "on";
	private static final String[] KEYS_ON = new String[] { KEY_ON };
	private static final String[] KEYS_YES_NO = new String[] { "yes", "no" };
	private static final String KEY_MULTI_PARTICIPATION = "multi.participation";
	private static final String KEY_COACH_CONFIRMATION = "coach.confirmation";
	private static final String KEY_PARTICIPATION_VISIBLE = "participation.visible";
	private static final String CMD_START_DURATION_REMOVE = "start.duration.remove";
	private static final String CMD_START_DURATION_ADD = "start.duration.add";
	private static final String CMD_START_END_REMOVE = "start.end.remove";
	private static final String CMD_START_END_ADD = "start.end.add";
	
	public enum AppointmentInputType { startDuration, startEnd, recurring }
	
	private TextElement titleEl;
	private TextElement descriptionEl;
	private SingleSelection typeEl;
	private MultipleSelectionElement configurationEl;
	private MultipleSelectionElement organizerEl;
	private TextElement locationEl;
	private TextElement maxParticipationsEl;
	private TextElement detailsEl;
	private DateChooser recurringFirstEl;
	private SingleSelection appointmentInputTypeEl;
	private FormLayoutContainer startDurationCont;
	private FormLayoutContainer startEndCont;
	private MultipleSelectionElement recurringDaysOfWeekEl;
	private DateChooser recurringLastEl;
	private SpacerElement bbbSpacer;
	private MultipleSelectionElement bbbRoomEl;
	private TextElement externalLinkEl;
	private FormLink openCalLink;
	private TextElement leadTimeEl;
	private TextElement followupTimeEl;
	private TextElement welcomeEl;
	private SingleSelection templateEl;
	private SingleSelection recordEl;
	private SingleSelection layoutEl;
	
	private BigBlueButtonMeetingsCalendarController calCtr;
	private CloseableModalController cmc;
	
	private RepositoryEntry entry;
	private String subIdent;
	private Topic topic;
	private List<Identity> coaches;
	private AppointmentInputType appointmentInputType;
	private List<BigBlueButtonMeetingTemplate> templates;
	private List<AppointmentWrapper> startDurationWrappers = new ArrayList<>();
	private List<AppointmentWrapper> startEndWrappers = new ArrayList<>();
	private boolean multiParticipationsSelected = true;
	private boolean coachConfirmationSelected = true;
	private boolean participationVisible = true;
	private int counter = 0;
	
	@Autowired
	private AppointmentsService appointmentsService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private UserManager userManager;

	public AppointmentCreateController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry,
			String subIdent) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(EditBigBlueButtonMeetingController.class, getLocale(), getTranslator()));
		this.entry = entry;
		this.subIdent = subIdent;
		this.appointmentInputType = AppointmentInputType.startDuration;
		
		coaches = repositoryService.getMembers(entry, RepositoryEntryRelationType.all, GroupRoles.coach.name());
		
		initForm(ureq);
		updateUI();
	}
	
	public AppointmentCreateController(UserRequest ureq, WindowControl wControl, Topic topic, AppointmentInputType appointmentInputType) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(EditBigBlueButtonMeetingController.class, getLocale(), getTranslator()));
		this.entry = topic.getEntry();
		this.subIdent = topic.getSubIdent();
		this.topic = topic;
		this.appointmentInputType  = appointmentInputType;
		
		initForm(ureq);
		updateUI();
	}
	
	public Topic getTopic() {
		return topic;
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if (topic == null) {
			// Topic
			titleEl = uifactory.addTextElement("topic.title", "topic.title", 128, null, formLayout);
			titleEl.setMandatory(true);
			
			descriptionEl = uifactory.addTextAreaElement("topic.description", "topic.description", 2000, 3, 72, false,
					false, null, formLayout);
			
			// Configs
			KeyValues typeKV = new KeyValues();
			typeKV.add(entry(Topic.Type.enrollment.name(), translate("topic.type.enrollment")));
			typeKV.add(entry(Topic.Type.finding.name(), translate("topic.type.finding")));
			typeEl = uifactory.addRadiosHorizontal("topic.type", formLayout, typeKV.keys(), typeKV.values());
			typeEl.select(Topic.Type.enrollment.name(), true);
			typeEl.addActionListener(FormEvent.ONCHANGE);
			
			configurationEl = uifactory.addCheckboxesVertical("topic.configuration", formLayout, emptyStrings(),
					emptyStrings(), 1);
			configurationEl.addActionListener(FormEvent.ONCHANGE);
			
			// Organizer
			KeyValues coachesKV = new KeyValues();
			for (Identity coach : coaches) {
				coachesKV.add(entry(coach.getKey().toString(), userManager.getUserDisplayName(coach.getKey())));
			}
			coachesKV.sort(VALUE_ASC);
			organizerEl = uifactory.addCheckboxesDropdown("organizer", "organizer", formLayout, coachesKV.keys(), coachesKV.values());
			organizerEl.setVisible(!coaches.isEmpty());
			
			if (organizerEl.isVisible()) {
				String defaultOrganizerKey = getIdentity().getKey().toString();
				if (organizerEl.getKeys().contains(defaultOrganizerKey)) {
						organizerEl.select(defaultOrganizerKey, true);
				}
			}
		}
		
		// Appointments
		locationEl = uifactory.addTextElement("appointment.location", 128, null, formLayout);
		if (topic == null) {
			locationEl.setHelpTextKey("appointment.init.value", null);
		}
		
		maxParticipationsEl = uifactory.addTextElement("appointment.max.participations", 5, null, formLayout);
		if (topic == null) {
			maxParticipationsEl.setHelpTextKey("appointment.init.value", null);
		}
		
		if (topic != null) {
			detailsEl = uifactory.addTextAreaElement("appointment.details", "appointment.details", 2000, 4, 72, false,
					false, null, formLayout);
		}
		
		if (topic == null) {
			KeyValues inputKV = new KeyValues();
			inputKV.add(KeyValues.entry(AppointmentInputType.startDuration.name(), translate("appointment.input.start.duration")));
			inputKV.add(KeyValues.entry(AppointmentInputType.startEnd.name(), translate("appointment.input.start.end")));
			inputKV.add(KeyValues.entry(AppointmentInputType.recurring.name(), translate("appointment.input.recurring")));
			appointmentInputTypeEl = uifactory.addRadiosHorizontal("appointment.input.type", formLayout, inputKV.keys(), inputKV.values());
			appointmentInputTypeEl.select(appointmentInputType.name(), true);
			appointmentInputTypeEl.addActionListener(FormEvent.ONCHANGE);
		}
		
		// Appointments with start / duration
		startDurationCont = FormLayoutContainer.createCustomFormLayout("startDurationCont", getTranslator(), velocity_root + "/appointments_single.html");
		formLayout.add(startDurationCont);
		startDurationCont.setRootForm(mainForm);
		startDurationCont.setLabel("appointments", null);
		
		doCreateStartDurationWrapper(null);
		startDurationCont.contextPut("appointments", startDurationWrappers);
		
		// Appointments with start / end
		startEndCont = FormLayoutContainer.createCustomFormLayout("startEndCont", getTranslator(), velocity_root + "/appointments_single.html");
		formLayout.add(startEndCont);
		startEndCont.setRootForm(mainForm);
		startEndCont.setLabel("appointments", null);
		
		doCreateStartEndWrapper(null);
		startEndCont.contextPut("appointments", startEndWrappers);
		
		// Recurring appointments
		recurringFirstEl = uifactory.addDateChooser("appointments.recurring.first", null, formLayout);
		recurringFirstEl.setDateChooserTimeEnabled(true);
		recurringFirstEl.setSecondDate(true);
		recurringFirstEl.setSameDay(true);
		recurringFirstEl.setMandatory(true);
		
		DayOfWeek[] dayOfWeeks = DayOfWeek.values();
		KeyValues dayOfWeekKV = new KeyValues();
		for (int i = 0; i < dayOfWeeks.length; i++) {
			dayOfWeekKV.add(entry(dayOfWeeks[i].name(), dayOfWeeks[i].getDisplayName(TextStyle.FULL_STANDALONE, getLocale())));
		}
		recurringDaysOfWeekEl = uifactory.addCheckboxesHorizontal("appointments.recurring.days.of.week", formLayout,
				dayOfWeekKV.keys(), dayOfWeekKV.values());
		recurringDaysOfWeekEl.setMandatory(true);
		
		recurringLastEl = uifactory.addDateChooser("appointments.recurring.last", null, formLayout);
		recurringLastEl.setMandatory(true);
		
		if (appointmentsService.isBigBlueButtonEnabled()) {
			bbbSpacer = uifactory.addSpacerElement("bbb.spacer", formLayout, false);
			
			String[] onValues = TranslatorHelper.translateAll(getTranslator(), KEYS_ON);
			bbbRoomEl = uifactory.addCheckboxesHorizontal("appointment.bbb.room", "appointment.bbb.room", formLayout, KEYS_ON, onValues);
			bbbRoomEl.addActionListener(FormEvent.ONCHANGE);
			
			welcomeEl = uifactory.addRichTextElementForStringDataMinimalistic("meeting.welcome", "meeting.welcome", "", 8, 60, formLayout, getWindowControl());
			
			KeyValues templatesKV = new KeyValues();
			templates = appointmentsService.getBigBlueButtonTemplates(entry, getIdentity(), ureq.getUserSession().getRoles(), null);
			templates.forEach(template -> templatesKV.add(KeyValues.entry(template.getKey().toString(), template.getName())));
			templatesKV.sort(KeyValues.VALUE_ASC);
			templateEl = uifactory.addDropdownSingleselect("meeting.template", "meeting.template", formLayout,
					templatesKV.keys(), templatesKV.values());
			templateEl.addActionListener(FormEvent.ONCHANGE);
			if(!templatesKV.isEmpty()) {
				templateEl.select(templateEl.getKeys()[0], true);
			}
			
			String[] yesNoValues = new String[] { translate("yes"), translate("no")  };
			recordEl = uifactory.addRadiosVertical("meeting.record", formLayout, KEYS_YES_NO, yesNoValues);
			recordEl.select(KEYS_YES_NO[0], true);
		
			KeyValues layoutKeyValues = new KeyValues();
			layoutKeyValues.add(KeyValues.entry(BigBlueButtonMeetingLayoutEnum.standard.name(), translate("layout.standard")));
			if(isWebcamLayoutAvailable(getSelectedTemplate(templateEl, templates))) {
				layoutKeyValues.add(KeyValues.entry(BigBlueButtonMeetingLayoutEnum.webcam.name(), translate("layout.webcam")));
			}
			layoutEl = uifactory.addDropdownSingleselect("meeting.layout", "meeting.layout", formLayout,
					layoutKeyValues.keys(), layoutKeyValues.values());
			boolean layoutSelected = false;
			if(!layoutSelected) {
				layoutEl.select(BigBlueButtonMeetingLayoutEnum.standard.name(), true);
			}
			layoutEl.setVisible(layoutEl.getKeys().length > 1);
			
			String externalLink = CodeHelper.getForeverUniqueID() + "";
			externalLinkEl = uifactory.addTextElement("meeting.external.users", 64, externalLink, formLayout);
			externalLinkEl.setPlaceholderKey("meeting.external.users.empty", null);
			externalLinkEl.setHelpTextKey("meeting.external.users.help", null);
			externalLinkEl.addActionListener(FormEvent.ONCHANGE);
			externalLinkEl.setExampleKey("noTransOnlyParam", new String[] {BigBlueButtonDispatcher.getMeetingUrl(externalLink)});
			
			openCalLink = uifactory.addFormLink("calendar.open", formLayout);
			openCalLink.setIconLeftCSS("o_icon o_icon-fw o_icon_calendar");
			BigBlueButtonUIHelper.updateTemplateInformations(templateEl, externalLinkEl, null, recordEl, templates);
			
			leadTimeEl = uifactory.addTextElement("meeting.leadTime", 8, null, formLayout);
			leadTimeEl.setExampleKey("meeting.leadTime.explain", null);
			
			followupTimeEl = uifactory.addTextElement("meeting.followupTime", 8, null, formLayout);
		}
		
		// Buttons
		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonsCont);
		buttonsCont.setRootForm(mainForm);
		uifactory.addFormSubmitButton("save", buttonsCont);
		uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
	}

	private void updateUI() {
		boolean enrollment = isEnrollment();
		
		if (topic == null) {
			KeyValues configKV = new KeyValues();
			configKV.add(entry(KEY_MULTI_PARTICIPATION, translate("topic.multi.participation")));
			if (enrollment) {
				configKV.add(entry(KEY_COACH_CONFIRMATION, translate("topic.coach.confirmation")));
			}
			configKV.add(entry(KEY_PARTICIPATION_VISIBLE, translate("topic.participation.visible")));
			configurationEl.setKeysAndValues(configKV.keys(), configKV.values());
			configurationEl.select(KEY_MULTI_PARTICIPATION, multiParticipationsSelected);
			configurationEl.select(KEY_COACH_CONFIRMATION, coachConfirmationSelected);
			configurationEl.select(KEY_PARTICIPATION_VISIBLE, participationVisible);
		}
		
		maxParticipationsEl.setVisible(enrollment);
		
		startDurationCont.setVisible(AppointmentInputType.startDuration == appointmentInputType);
		
		startEndCont.setVisible(AppointmentInputType.startEnd == appointmentInputType);
		
		boolean recurring = AppointmentInputType.recurring == appointmentInputType;
		recurringFirstEl.setVisible(recurring);
		recurringDaysOfWeekEl.setVisible(recurring);
		recurringLastEl.setVisible(recurring);
		
		if (bbbRoomEl != null) {
			boolean bbbRoom = bbbRoomEl.isAtLeastSelected(1);
			bbbSpacer.setVisible(bbbRoom);
			templateEl.setVisible(bbbRoom);
			externalLinkEl.setVisible(bbbRoom);
			openCalLink.setVisible(bbbRoom);
			leadTimeEl.setVisible(bbbRoom);
			followupTimeEl.setVisible(bbbRoom);
			welcomeEl.setVisible(bbbRoom);
			templateEl.setVisible(bbbRoom);
			recordEl.setVisible(bbbRoom && BigBlueButtonUIHelper.isRecord(getSelectedTemplate(templateEl, templates)));
			layoutEl.setVisible(bbbRoom);
		}
	}

	private boolean isEnrollment() {
		return topic == null
				? typeEl.isOneSelected() && Type.valueOf(typeEl.getSelectedKey()) != Type.finding
				: topic.getType() != Type.finding;
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (calCtr == source) {
			cmc.deactivate();
			cleanUp();
		} else if (cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(calCtr);
		removeAsListenerAndDispose(cmc);
		calCtr = null;
		cmc = null;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == typeEl) {
			updateUI();
		} else if (source == configurationEl) {
			Collection<String> configKeys = configurationEl.getSelectedKeys();
			multiParticipationsSelected = configKeys.contains(KEY_MULTI_PARTICIPATION);
			coachConfirmationSelected = configKeys.contains(KEY_COACH_CONFIRMATION);
			participationVisible = configKeys.contains(KEY_PARTICIPATION_VISIBLE);
		} else if (source == appointmentInputTypeEl) {
			if (appointmentInputTypeEl.isOneSelected()) {
				appointmentInputType = AppointmentInputType.valueOf(appointmentInputTypeEl.getSelectedKey());
			}
			updateUI();
		} else if (source == bbbRoomEl) {
			updateUI();
		} else if (templateEl == source) {
			BigBlueButtonUIHelper.updateTemplateInformations(templateEl, externalLinkEl, null, recordEl, templates);
			boolean webcamAvailable = isWebcamLayoutAvailable(getSelectedTemplate(templateEl, templates));
			BigBlueButtonUIHelper.updateLayoutSelection(layoutEl, getTranslator(), webcamAvailable);
		} else if (openCalLink == source) {
			doOpenCalendar(ureq);
		} else if (externalLinkEl == source) {
			BigBlueButtonUIHelper.validateReadableIdentifier(externalLinkEl, null);
		} else if (source instanceof DateChooser) {
			DateChooser dateChooser = (DateChooser)source;
			AppointmentWrapper wrapper = (AppointmentWrapper)dateChooser.getUserObject();
			doSetEndDate(wrapper);
		} else if (source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if (CMD_START_DURATION_ADD.equals(cmd)) {
				AppointmentWrapper wrapper = (AppointmentWrapper)link.getUserObject();
				doCreateStartDurationWrapper(wrapper);
			} else if (CMD_START_DURATION_REMOVE.equals(cmd)) {
				AppointmentWrapper wrapper = (AppointmentWrapper)link.getUserObject();
				doRemoveAppointmentWrapper(startDurationWrappers, wrapper);
			} else if (CMD_START_END_ADD.equals(cmd)) {
				AppointmentWrapper wrapper = (AppointmentWrapper)link.getUserObject();
				doCreateStartEndWrapper(wrapper);
			} else if (CMD_START_END_REMOVE.equals(cmd)) {
				AppointmentWrapper wrapper = (AppointmentWrapper)link.getUserObject();
				doRemoveAppointmentWrapper(startEndWrappers, wrapper);
			}
		} else if (source instanceof TextElement) {
			if (source.getName().startsWith(CMD_DURATION)) {
				AppointmentWrapper wrapper = (AppointmentWrapper)source.getUserObject();
				doSetEndDate(wrapper);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = super.validateFormLogic(ureq);
		
		if (titleEl != null) {
			titleEl.clearError();
			if(!StringHelper.containsNonWhitespace(titleEl.getValue())) {
				titleEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			}
		}
		
		maxParticipationsEl.clearError();
		String maxParticipationsValue = maxParticipationsEl.getValue();
		Integer maxParticipants = null;
		if (maxParticipationsEl.isVisible() && StringHelper.containsNonWhitespace(maxParticipationsValue)) {
			try {
				maxParticipants = Integer.parseInt(maxParticipationsValue);
				if (maxParticipants.intValue() < 1) {
					maxParticipationsEl.setErrorKey("error.positiv.number", null);
					allOk &= false;
				}
			} catch (NumberFormatException e) {
				maxParticipationsEl.setErrorKey("error.positiv.number", null);
				allOk &= false;
			}
		}
		
		for (AppointmentWrapper wrapper : startDurationWrappers) {
			DateChooser startEl = wrapper.getStartEl();
			if (AppointmentInputType.startDuration == appointmentInputType) {
				if (wrapper.getStartEl().getDate() != null && !StringHelper.containsNonWhitespace(wrapper.getDurationEl().getValue())) {
					startEl.setErrorKey("form.legende.mandatory", null);
					allOk &= false;
				} else if (wrapper.getStartEl().getDate() == null && StringHelper.containsNonWhitespace(wrapper.getDurationEl().getValue())) {
					startEl.setErrorKey("form.legende.mandatory", null);
					allOk &= false;
				} else if (StringHelper.containsNonWhitespace(wrapper.getDurationEl().getValue())) {
					try {
						Integer duration = Integer.parseInt(wrapper.getDurationEl().getValue());
						if (duration.intValue() < 1) {
							startEl.setErrorKey("error.positiv.number", null);
							allOk &= false;
						}
					} catch (NumberFormatException e) {
						startEl.setErrorKey("error.positiv.number", null);
						allOk &= false;
					}
				}
			}
		}
		
		for (AppointmentWrapper wrapper : startEndWrappers) {
			DateChooser startEl = wrapper.getStartEl();
			DateChooser endEl = wrapper.getEndEl();
			startEl.clearError();
			endEl.clearError();
			if (AppointmentInputType.startEnd == appointmentInputType) {
				if (startEl.getDate() == null && endEl.getDate() != null) {
					startEl.setErrorKey("form.legende.mandatory", null);
					allOk &= false;
				}
				if (endEl.getDate() == null && startEl.getDate() != null) {
					endEl.setErrorKey("form.legende.mandatory", null);
					allOk &= false;
				}
				if (startEl.getDate() != null && endEl.getDate() != null) {
					Date start = startEl.getDate();
					Date end = endEl.getDate();
					if(end.before(start)) {
						endEl.setErrorKey("error.start.after.end", null);
						allOk &= false;
					}
				}
			}
		}
		
		recurringFirstEl.clearError();
		recurringDaysOfWeekEl.clearError();
		recurringLastEl.clearError();
		if (AppointmentInputType.recurring == appointmentInputType) {
			if (recurringFirstEl.getDate() == null || recurringFirstEl.getSecondDate() == null) {
				recurringFirstEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			} else if (recurringFirstEl.getDate().after(recurringFirstEl.getSecondDate())) {
				recurringFirstEl.setErrorKey("error.start.after.end", null);
				allOk &= false;
			}
			
			if (!recurringDaysOfWeekEl.isAtLeastSelected(1)) {
				recurringDaysOfWeekEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			}
			
			if (recurringLastEl.getDate() == null) {
				recurringLastEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			}
			
			if (recurringFirstEl.getDate() != null && recurringLastEl.getDate() != null
					&& recurringFirstEl.getDate().after(recurringLastEl.getDate())) {
				recurringLastEl.setErrorKey("error.first.after.start", null);
				allOk &= false;
			}
		}
		
		boolean bbbOk = true;
		if (templateEl != null && templateEl.isVisible()) {
			bbbOk &= BigBlueButtonUIHelper.validateReadableIdentifier(externalLinkEl, null);
			
			bbbOk &= BigBlueButtonUIHelper.validateTime(leadTimeEl, 15l);
			bbbOk &= BigBlueButtonUIHelper.validateTime(followupTimeEl, 15l);
			
			templateEl.clearError();
			if(!templateEl.isOneSelected()) {
				if(templateEl.getKeys() == null || templateEl.getKeys().length == 0) {
					templateEl.setErrorKey("error.bigbluebutton.no.templates", null);
				} else {
					templateEl.setErrorKey("form.legende.mandatory", null);
				}
				bbbOk &= false;
			}
			
			// dates ok
			if(bbbOk) {
				BigBlueButtonMeetingTemplate template = BigBlueButtonUIHelper.getSelectedTemplate(templateEl, templates);
				if (!maxParticipationsEl.hasError()){
					if (maxParticipants == null) {
						maxParticipationsEl.setValue(template.getMaxParticipants().toString());
					} else if (maxParticipants.intValue() > template.getMaxParticipants().intValue()) {
						maxParticipationsEl.setErrorKey("error.participations.max.greater.room", new String[] {template.getMaxParticipants().toString()});
						bbbOk &= false;
					}
				}
				
				if (AppointmentInputType.recurring == appointmentInputType) {
					allOk &= BigBlueButtonUIHelper.validateDuration(recurringFirstEl, leadTimeEl, followupTimeEl, template);
					if (!recurringFirstEl.hasError() && !validateRecurringSlot(template)) {
						recurringFirstEl.setErrorKey("server.overloaded", new String[] { null });
						bbbOk &= false;
					}
				} else {
					List<AppointmentWrapper> wrappers = AppointmentInputType.startDuration == appointmentInputType? startDurationWrappers: startEndWrappers;
					for (AppointmentWrapper wrapper : wrappers) {
						DateChooser startEl = wrapper.getStartEl();
						DateChooser endEl = wrapper.getEndEl();
						startEl.clearError();
						endEl.clearError();
						if (startEl.getDate() != null && endEl.getDate() != null) {
							if (!BigBlueButtonUIHelper.validateDuration(startEl, leadTimeEl, endEl, followupTimeEl, template)) {
								bbbOk &= false;
							}
							if (!BigBlueButtonUIHelper.validateSlot(startEl, leadTimeEl, endEl, followupTimeEl, null, template)) {
								bbbOk &= false;
							}
						}
					}
				}
			}
		}
		if (!bbbOk) {
			allOk &= false;
		}
		
		return allOk;
	}
	
	private boolean validateRecurringSlot(BigBlueButtonMeetingTemplate template) {
		long leadTime = BigBlueButtonUIHelper.getLongOrZero(leadTimeEl);
		long followupTime = BigBlueButtonUIHelper.getLongOrZero(followupTimeEl);
		
		Date firstStart = recurringFirstEl.getDate();
		Date firstEnd = recurringFirstEl.getSecondDate();
		
		Date last = recurringLastEl.getDate();
		last = DateUtils.setTime(last, 23, 59, 59);
		
		Collection<DayOfWeek> daysOfWeek = recurringDaysOfWeekEl.getSelectedKeys().stream()
				.map(DayOfWeek::valueOf)
				.collect(Collectors.toList());
		
		List<Date> starts = DateUtils.getDaysInRange(firstStart, last, daysOfWeek);
		for (Date start : starts) {
			Date end = DateUtils.copyTime(start, firstEnd);
			boolean valid = BigBlueButtonUIHelper.validateSlot(null, template, start, end, leadTime, followupTime);
			if (!valid) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSave();
		fireEvent(ureq, Event.DONE_EVENT);
	}
	
	private void doSave() {
		if (topic == null) {
			doSaveTopic();
			doSaveOrganizers();
		}
		
		switch (appointmentInputType) {
		case startDuration:
			doSaveWrappedAppointments(startDurationWrappers);
			break;
		case startEnd:
			doSaveWrappedAppointments(startEndWrappers);
			break;
		case recurring:
			doSaveReccuringAppointments();
			break;
		default:
			break;
		}
	}

	private void doSaveTopic() {
		topic = appointmentsService.createTopic(entry, subIdent);
		
		String title = titleEl.getValue();
		topic.setTitle(title);
		
		String description = descriptionEl.getValue();
		topic.setDescription(description);

		Type type = typeEl.isOneSelected() ? Type.valueOf(typeEl.getSelectedKey()) : Type.enrollment;
		topic.setType(type);
		
		Collection<String> configKeys = configurationEl.getSelectedKeys();
		boolean multiParticipation = configKeys.contains(KEY_MULTI_PARTICIPATION);
		topic.setMultiParticipation(multiParticipation);
		
		boolean autoConfirmation = Type.finding == type
				? false
				: !configKeys.contains(KEY_COACH_CONFIRMATION);
		topic.setAutoConfirmation(autoConfirmation);
		
		boolean participationVisible = configKeys.contains(KEY_PARTICIPATION_VISIBLE);
		topic.setParticipationVisible(participationVisible);
		
		topic = appointmentsService.updateTopic(topic);
	}

	private void doSaveOrganizers() {
		Collection<String> selectedOrganizerKeys = organizerEl.getSelectedKeys();
		List<Identity> selectedOrganizers = coaches.stream()
				.filter(i -> selectedOrganizerKeys.contains(i.getKey().toString()))
				.collect(Collectors.toList());
		appointmentsService.updateOrganizers(topic, selectedOrganizers);
	}

	private void doSaveWrappedAppointments(Collection<AppointmentWrapper> appointmentWrappers) {
		for (AppointmentWrapper wrapper : appointmentWrappers) {
			DateChooser startEl = wrapper.getStartEl();
			DateChooser endEl = wrapper.getEndEl();
			if (startEl.getDate() != null && endEl.getDate() != null) {
				Appointment appointment = appointmentsService.createUnsavedAppointment(topic);
				
				Date start = startEl.getDate();
				appointment.setStart(start);
				
				Date end = endEl.getDate();
				appointment.setEnd(end);
				
				String location = locationEl.getValue();
				appointment.setLocation(location);
				
				if (detailsEl != null) {
					String details = detailsEl.getValue();
					appointment.setDetails(details);
				}
				
				if (maxParticipationsEl.isVisible()) {
					String maxParticipationsValue = maxParticipationsEl.getValue();
					Integer maxParticipations = StringHelper.containsNonWhitespace(maxParticipationsValue)
							? Integer.valueOf(maxParticipationsValue)
							: null;
					appointment.setMaxParticipations(maxParticipations);
				}
				
				appointment = addMeeting(appointment);
				appointmentsService.saveAppointment(appointment);
			}
		}
	}

	private void doSaveReccuringAppointments() {
		Date firstStart = recurringFirstEl.getDate();
		Date firstEnd = recurringFirstEl.getSecondDate();
		Date last = recurringLastEl.getDate();
		last = DateUtils.setTime(last, 23, 59, 59);
		
		Collection<DayOfWeek> daysOfWeek = recurringDaysOfWeekEl.getSelectedKeys().stream()
				.map(DayOfWeek::valueOf)
				.collect(Collectors.toList());
		
		List<Date> starts = DateUtils.getDaysInRange(firstStart, last, daysOfWeek);
		for (Date start : starts) {
			Appointment appointment = appointmentsService.createUnsavedAppointment(topic);
			
			appointment.setStart(start);
			
			Date end = DateUtils.copyTime(start, firstEnd);
			appointment.setEnd(end);
			
			String location = locationEl.getValue();
			appointment.setLocation(location);
			
			if (detailsEl != null) {
				String details = detailsEl.getValue();
				appointment.setDetails(details);
			}
			
			if (maxParticipationsEl.isVisible()) {
				String maxParticipationsValue = maxParticipationsEl.getValue();
				Integer maxParticipations = StringHelper.containsNonWhitespace(maxParticipationsValue)
						? Integer.valueOf(maxParticipationsValue)
						: null;
				appointment.setMaxParticipations(maxParticipations);
			}
			
			appointment = addMeeting(appointment);
			appointmentsService.saveAppointment(appointment);
		}
	}

	private Appointment addMeeting(Appointment appointment) {
		if (bbbRoomEl != null && bbbRoomEl.isAtLeastSelected(1)) {
			appointment = appointmentsService.addMeeting(appointment, getIdentity());
			BigBlueButtonMeeting meeting = appointment.getMeeting();
			
			String mainPresenters = appointmentsService.getMainPresenters(topic);
			meeting.setMainPresenter(mainPresenters);
			
			meeting.setName(topic.getTitle());
			meeting.setDescription(topic.getDescription());
			meeting.setWelcome(welcomeEl.getValue());
			BigBlueButtonMeetingTemplate template = getSelectedTemplate(templateEl, templates);
			meeting.setTemplate(template);
			
			if(template != null && template.isExternalUsersAllowed()
					&& externalLinkEl.isVisible() && StringHelper.containsNonWhitespace(externalLinkEl.getValue())) {
				meeting.setReadableIdentifier(externalLinkEl.getValue());
			} else {
				meeting.setReadableIdentifier(null);
			}
			
			meeting.setPermanent(false);
		
			meeting.setStartDate(appointment.getStart());
			meeting.setEndDate(appointment.getEnd());
			long leadTime = BigBlueButtonUIHelper.getLongOrZero(leadTimeEl);
			meeting.setLeadTime(leadTime);
			long followupTime = BigBlueButtonUIHelper.getLongOrZero(followupTimeEl);
			meeting.setFollowupTime(followupTime);
			
			if(layoutEl.isVisible() && layoutEl.isOneSelected()) {
				BigBlueButtonMeetingLayoutEnum layout = BigBlueButtonMeetingLayoutEnum.secureValueOf(layoutEl.getSelectedKey());
				meeting.setMeetingLayout(layout);
			} else {
				meeting.setMeetingLayout(BigBlueButtonMeetingLayoutEnum.standard);
			}
			
			if(recordEl.isVisible() && recordEl.isOneSelected()) {
				meeting.setRecord(Boolean.valueOf(KEYS_YES_NO[0].equals(recordEl.getSelectedKey())));
			} else {
				meeting.setRecord(null);
			}
		}
		return appointment;
	}
	
	private void doCreateStartDurationWrapper(AppointmentWrapper after) {
		AppointmentWrapper wrapper = new AppointmentWrapper();
		
		StartDuration previous = none();
		StartDuration previous2 = none();
		if (after != null) {
			previous = ofString(after.getStartEl().getDate(), after.getDurationEl().getValue());
			int index = startDurationWrappers.indexOf(after);
			if (index >= 1) {
				AppointmentWrapper after2 = startDurationWrappers.get(index - 1);
				previous2 = ofString(after2.getStartEl().getDate(), after2.getDurationEl().getValue());
			}
		}
		StartDuration next = StartDuration.next(previous, previous2);
		
		DateChooser startEl = uifactory.addDateChooser("start_" + counter++, null, next.getStart(), startDurationCont);
		startEl.setDateChooserTimeEnabled(true);
		startEl.setUserObject(wrapper);
		startEl.addActionListener(FormEvent.ONCHANGE);
		wrapper.setStartEl(startEl);
		
		String duration = next.getDuration() != null? next.getDuration().toString(): null;
		TextElement durationEl = uifactory.addTextElement(CMD_DURATION + counter++, 2, duration, startDurationCont);
		durationEl.setDisplaySize(1);
		durationEl.addActionListener(FormEvent.ONCHANGE);
		durationEl.setUserObject(wrapper);
		wrapper.setDurationEl(durationEl);
		
		Date end = getEnd(next);
		DateChooser endEl = uifactory.addDateChooser("end_" + counter++, null, end, startDurationCont);
		endEl.setTimeOnly(true);
		endEl.setEnabled(false);
		wrapper.setEndEl(endEl);
		
		FormLink addEl = uifactory.addFormLink("add_" + counter++, CMD_START_DURATION_ADD, "", null, startDurationCont, Link.NONTRANSLATED + Link.BUTTON);
		addEl.setIconLeftCSS("o_icon o_icon-lg o_icon_add");
		addEl.setUserObject(wrapper);
		wrapper.setAddEl(addEl);
		
		FormLink removeEl = uifactory.addFormLink("remove_" + counter++, CMD_START_DURATION_REMOVE, "", null, startDurationCont, Link.NONTRANSLATED + Link.BUTTON);
		removeEl.setIconLeftCSS("o_icon o_icon-lg o_icon_delete");
		removeEl.setUserObject(wrapper);
		wrapper.setRemoveEl(removeEl);
		
		if (after == null) {
			startDurationWrappers.add(wrapper);
		} else {
			int index = startDurationWrappers.indexOf(after) + 1;
			startDurationWrappers.add(index, wrapper);
		}
		showHideRemoveButtons(startDurationWrappers);
	}
	
	private void doCreateStartEndWrapper(AppointmentWrapper after) {
		AppointmentWrapper wrapper = new AppointmentWrapper();
		
		DateChooser startEl = uifactory.addDateChooser("start_" + counter++, null, null, startEndCont);
		startEl.setDateChooserTimeEnabled(true);
		startEl.setUserObject(wrapper);
		startEl.addActionListener(FormEvent.ONCHANGE);
		wrapper.setStartEl(startEl);
		
		DateChooser endEl = uifactory.addDateChooser("end_" + counter++, null, null, startEndCont);
		endEl.setDateChooserTimeEnabled(true);
		wrapper.setEndEl(endEl);
		
		FormLink addEl = uifactory.addFormLink("add_" + counter++, CMD_START_END_ADD, "", null, startEndCont, Link.NONTRANSLATED + Link.BUTTON);
		addEl.setIconLeftCSS("o_icon o_icon-lg o_icon_add");
		addEl.setUserObject(wrapper);
		wrapper.setAddEl(addEl);
		
		FormLink removeEl = uifactory.addFormLink("remove_" + counter++, CMD_START_END_REMOVE, "", null, startEndCont, Link.NONTRANSLATED + Link.BUTTON);
		removeEl.setIconLeftCSS("o_icon o_icon-lg o_icon_delete");
		removeEl.setUserObject(wrapper);
		wrapper.setRemoveEl(removeEl);
		
		if (after == null) {
			startEndWrappers.add(wrapper);
		} else {
			Date start = after.getStartEl().getDate();
			startEl.setDate(start);
			Date end = after.getEndEl().getDate();
			endEl.setDate(end);
			int index = startEndWrappers.indexOf(after) + 1;
			startEndWrappers.add(index, wrapper);
		}
		showHideRemoveButtons(startEndWrappers);
	}

	private void doRemoveAppointmentWrapper(Collection<AppointmentWrapper> appointmentWrappers, AppointmentWrapper wrapper) {
		appointmentWrappers.remove(wrapper);
		showHideRemoveButtons(appointmentWrappers);
	}

	private void showHideRemoveButtons(Collection<AppointmentWrapper> appointmentWrappers) {
		boolean enabled = appointmentWrappers.size() != 1;
		appointmentWrappers.stream()
				.forEach(wrapper -> wrapper.getRemoveEl().setEnabled(enabled));
	}

	private void doSetEndDate(AppointmentWrapper wrapper) {
		DateChooser startEl = wrapper.getStartEl();
		if (startEl != null) {
			TextElement durationEl = wrapper.getDurationEl();
			DateChooser endEl = wrapper.getEndEl();
			if (durationEl != null) {
				Date end = getEnd(ofString(startEl.getDate(), durationEl.getValue()));
				endEl.setDate(end);
			} else if (endEl != null && startEl.getDate() != null && endEl.getDate() == null) {
				endEl.setDate(startEl.getDate());
			}
		}
	}
	
	private void doOpenCalendar(UserRequest ureq) {
		removeAsListenerAndDispose(calCtr);
		removeAsListenerAndDispose(cmc);

		calCtr = new BigBlueButtonMeetingsCalendarController(ureq, getWindowControl());
		listenTo(calCtr);
		cmc = new CloseableModalController(getWindowControl(), "close", calCtr.getInitialComponent(), true,
				translate("calendar.open"));
		cmc.activate();
		listenTo(cmc);
	}

	@Override
	protected void doDispose() {
		//
	}
	
	public static final class AppointmentWrapper {
		
		private DateChooser startEl;
		private TextElement durationEl;
		private DateChooser endEl;
		private FormLink addEl;
		private FormLink removeEl;

		public String getStartElName() {
			return startEl != null? startEl.getComponent().getComponentName(): null;
		}
		
		public DateChooser getStartEl() {
			return startEl;
		}
		
		public void setStartEl(DateChooser startEl) {
			this.startEl = startEl;
		}
		
		public String getDurationElName() {
			return durationEl != null? durationEl.getComponent().getComponentName(): null;
		}
		
		public TextElement getDurationEl() {
			return durationEl;
		}

		public void setDurationEl(TextElement durationEl) {
			this.durationEl = durationEl;
		}

		public String getEndElName() {
			return endEl != null? endEl.getComponent().getComponentName(): null;
		}
		
		public DateChooser getEndEl() {
			return endEl;
		}
		
		public void setEndEl(DateChooser endEl) {
			this.endEl = endEl;
		}
		
		public String getRemoveElName() {
			return removeEl != null? removeEl.getComponent().getComponentName(): null;
		}

		public FormLink getRemoveEl() {
			return removeEl;
		}

		public void setRemoveEl(FormLink removeEl) {
			this.removeEl = removeEl;
		}
		
		public String getAddElName() {
			return addEl != null? addEl.getComponent().getComponentName(): null;
		}

		public FormLink getAddEl() {
			return addEl;
		}

		public void setAddEl(FormLink addEl) {
			this.addEl = addEl;
		}
		
	}

}
