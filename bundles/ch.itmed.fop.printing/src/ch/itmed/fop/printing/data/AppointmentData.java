/*******************************************************************************
 * Copyright (c) 2019 IT-Med AG <info@it-med-ag.ch>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IT-Med AG <info@it-med-ag.ch> - initial implementation
 ******************************************************************************/

package ch.itmed.fop.printing.data;

import org.apache.commons.lang3.StringUtils;

import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.core.model.IAppointment;
import ch.elexis.core.model.IContact;
import ch.elexis.core.model.agenda.AreaType;
import ch.elexis.core.services.holder.ConfigServiceHolder;
import ch.elexis.core.services.holder.ContextServiceHolder;
import ch.elexis.core.services.holder.CoreModelServiceHolder;
import ch.rgw.tools.TimeTool;

public final class AppointmentData {
	private IAppointment appointment;

	public void load() throws NullPointerException {
		appointment = ContextServiceHolder.get().getTyped(IAppointment.class).orElse(null);
		if (appointment == null) {
			throw new NullPointerException("No appointment selected"); //$NON-NLS-1$
		}
	}

	public AppointmentData() {
	}

	public AppointmentData(IAppointment appointment) {
		this.appointment = appointment;
	}

	public String getAppointmentDetailed() {
		StringBuilder appointmentDate = new StringBuilder();

		// the weekday of the appointment
		TimeTool startTime = getStartTime();
		appointmentDate.append(startTime.toString(TimeTool.WEEKDAY));
		appointmentDate.append(", "); //$NON-NLS-1$

		// the date of the appointment
		appointmentDate.append(startTime.toString(TimeTool.DATE_GER));
		appointmentDate.append(StringUtils.SPACE);

		// start time of the appointment
		appointmentDate.append(startTime.toString(TimeTool.TIME_SMALL));
		appointmentDate.append(" - "); //$NON-NLS-1$

		// end time of the appointment
		TimeTool endTime = new TimeTool(appointment.getEndTime());
		appointmentDate.append(endTime.toString(TimeTool.TIME_SMALL));

		return appointmentDate.toString();
	}

	public String getAppointmentDetailedNoEnd() {
		StringBuilder appointmentDate = new StringBuilder();

		// the weekday of the appointment
		TimeTool startTime = getStartTime();
		appointmentDate.append(startTime.toString(TimeTool.WEEKDAY));
		appointmentDate.append(", "); //$NON-NLS-1$

		// the date of the appointment
		appointmentDate.append(startTime.toString(TimeTool.DATE_GER));
		appointmentDate.append(StringUtils.SPACE);

		// start time of the appointment
		appointmentDate.append(startTime.toString(TimeTool.TIME_SMALL));

		return appointmentDate.toString();
	}

	public TimeTool getStartTime() {
		return new TimeTool(appointment.getStartTime());
	}

	public String getAgendaArea() {
		IContact contact = null;
		String agendaSection = appointment.getSchedule();
		String type = ConfigServiceHolder.getGlobal(
				PreferenceConstants.AG_BEREICH_PREFIX + agendaSection + PreferenceConstants.AG_BEREICH_TYPE_POSTFIX,
				null);
		if (type != null) {
			if (type.startsWith(AreaType.CONTACT.name())) {
				contact = CoreModelServiceHolder.get()
						.load(type.substring(AreaType.CONTACT.name().length() + 1), IContact.class).orElse(null);
			}
		}

		if (contact != null) {
			return Salutation.getSalutation(contact);
		} else {
			return agendaSection;
		}
	}

}
