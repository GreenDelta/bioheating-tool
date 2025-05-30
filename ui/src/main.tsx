import * as React from 'react';
import { createRoot } from 'react-dom/client';

function main() {
	const container = document.getElementById('app');
	const root = createRoot(container!);
	root.render(<h1>Works!</h1>);
}
main();
