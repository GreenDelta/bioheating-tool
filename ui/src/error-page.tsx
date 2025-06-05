import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';

export const ErrorPage = () => {
	const [searchParams] = useSearchParams();
	const message = searchParams.get('message') || 'An unexpected error occurred';
	const details = searchParams.get('details');

	return (
		<div>
			<h1>Error</h1>
			<article>
				<p><strong>{message}</strong></p>
				{details && (
					<details>
						<summary>Error details</summary>
						<p style={{ fontFamily: 'monospace', fontSize: '0.9em' }}>
							{details}
						</p>
					</details>
				)}
				<footer>
					<Link to="/" role="button">
						Back to home page
					</Link>
				</footer>
			</article>
		</div>
	);
};
