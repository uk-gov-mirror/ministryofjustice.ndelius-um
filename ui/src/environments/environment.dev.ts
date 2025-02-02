import {AuthConfig} from 'angular-oauth2-oidc';

const authConfig: AuthConfig = {
  clientId: 'UserManagement-UI',
  dummyClientSecret: '',
  scope:
    // User management interactions (eg. add/update user):
    'UMBI001 UMBI002 UMBI003 UMBI004 UMBI005 UMBI006 UMBI007 UMBI008 UMBI009 UMBI010 UMBI011 UMBI012 ' +
    // Role management interactions (eg. public/private/national admin):
    'UABI020 UABI021 UABI022 UABI023 UABI024 UABI025 UABI026',
  resource: 'NDelius',
  responseType: 'code',
  useHttpBasicAuth: true,
  oidc: false,
  requireHttps: false,
  loginUrl: 'https://umt-dev.bconline.co.uk/umt/oauth/authorize',
  tokenEndpoint: 'https://umt-dev.bconline.co.uk/umt/oauth/token',
  redirectUri: window.location.origin + '/umt/',
};

export const environment = {
  production: false,
  api: {
    baseurl: 'https://umt-dev.bconline.co.uk/umt/api/'
  },
  authConfig: authConfig
};
