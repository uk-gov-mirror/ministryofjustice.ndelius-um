INSERT INTO ORGANISATION(ORGANISATION_ID, CODE, DESCRIPTION) VALUES (ORGANISATION_ID_SEQ.NEXTVAL, 'NPS', 'National Probation Service');

INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'N01', 'NPS London', NULL, ORGANISATION_ID_SEQ.CURRVAL);
INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'N02', 'NPS North East', NULL, ORGANISATION_ID_SEQ.CURRVAL);
INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'N03', 'NPS North West', NULL, ORGANISATION_ID_SEQ.CURRVAL);

INSERT INTO ORGANISATION(ORGANISATION_ID, CODE, DESCRIPTION) VALUES (ORGANISATION_ID_SEQ.NEXTVAL, 'PO1', 'Parent Organisation 1');

INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'C01', 'CRC London', (SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N01'), ORGANISATION_ID_SEQ.CURRVAL);
INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'C02', 'CRC North East', (SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N02'), ORGANISATION_ID_SEQ.CURRVAL);

INSERT INTO ORGANISATION(ORGANISATION_ID, CODE, DESCRIPTION) VALUES (ORGANISATION_ID_SEQ.NEXTVAL, 'PO2', 'Parent Organisation 2');

INSERT INTO PROBATION_AREA(PROBATION_AREA_ID, CODE, DESCRIPTION, DIVISION_ID, ORGANISATION_ID) VALUES (PROBATION_AREA_ID_SEQ.NEXTVAL, 'C03', 'CRC North West', (SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N03'), ORGANISATION_ID_SEQ.CURRVAL);

INSERT INTO R_STANDARD_REFERENCE_LIST (STANDARD_REFERENCE_LIST_ID, CODE_VALUE, CODE_DESCRIPTION, SELECTABLE) VALUES (STANDARD_REFERENCE_LIST_ID_SEQ.NEXTVAL, 'GRADE1', 'Grade 1', 'Y');
INSERT INTO R_STANDARD_REFERENCE_LIST (STANDARD_REFERENCE_LIST_ID, CODE_VALUE, CODE_DESCRIPTION, SELECTABLE) VALUES (STANDARD_REFERENCE_LIST_ID_SEQ.NEXTVAL, 'GRADE2', 'Grade 2', 'Y');

--INSERT INTO STAFF (STAFF_ID, FORENAME, FORENAME2, SURNAME, CODE, STAFF_GRADE_ID, START_DATE, END_DATE) VALUES (STAFF_ID_SEQ.NEXTVAL, 'Test', NULL, 'User', 'N01A001', (SELECT STANDARD_REFERENCE_LIST_ID FROM R_STANDARD_REFERENCE_LIST WHERE CODE = 'GRADE1'), SYSDATE-10, NULL);
--INSERT INTO USER_ (USER_ID, FORENAME, FORENAME2, SURNAME, END_DATE, DISTINGUISHED_NAME, ORGANISATION_ID, PRIVATE, STAFF_ID) VALUES (USER_ID_SEQ.NEXTVAL, 'Test', NULL, 'User', NULL, 'test.user', (SELECT ORGANISTION_ID FROM ORGANISATION WHERE CODE = 'NPS'), 0, (SELECT STAFF_ID FROM STAFF WHERE CODE = 'N01A001'));
INSERT INTO USER_ (USER_ID, FORENAME, FORENAME2, SURNAME, END_DATE, DISTINGUISHED_NAME, ORGANISATION_ID, PRIVATE, STAFF_ID) VALUES (USER_ID_SEQ.NEXTVAL, 'Test', NULL, 'User', NULL, 'test.user', (SELECT ORGANISATION_ID FROM ORGANISATION WHERE CODE = 'NPS'), 0, null);

INSERT INTO PROBATION_AREA_USER (PROBATION_AREA_ID, USER_ID) VALUES ((SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N01'), (SELECT USER_ID FROM USER_ WHERE DISTINGUISHED_NAME = 'test.user'));
INSERT INTO PROBATION_AREA_USER (PROBATION_AREA_ID, USER_ID) VALUES ((SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N02'), (SELECT USER_ID FROM USER_ WHERE DISTINGUISHED_NAME = 'test.user'));
INSERT INTO PROBATION_AREA_USER (PROBATION_AREA_ID, USER_ID) VALUES ((SELECT PROBATION_AREA_ID FROM PROBATION_AREA WHERE CODE = 'N03'), (SELECT USER_ID FROM USER_ WHERE DISTINGUISHED_NAME = 'test.user'));