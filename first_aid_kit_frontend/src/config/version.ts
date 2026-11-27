// Z package.json, żeby stopka pokazywała wersję wydania; zgodność z plikiem VERSION pilnuje version.test.ts.
import { version } from '../../package.json';

export const APP_VERSION = version;
export const APP_VERSION_LABEL = `Wersja ${APP_VERSION} | Stworzone z myślą o Twoim zdrowiu`;
