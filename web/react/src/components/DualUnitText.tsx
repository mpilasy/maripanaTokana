interface DualUnitTextProps {
	primary: string;
	secondary: string;
	primarySize?: string;
	align?: 'start' | 'end' | 'center';
	onClick?: () => void;
}

export default function DualUnitText({ primary, secondary, primarySize = '16px', align = 'start', onClick }: DualUnitTextProps) {
	return (
		<div
			className={`dual-unit${onClick ? ' clickable' : ''}`}
			style={{ textAlign: align }}
			onClick={onClick}
		>
			<div className="dual-unit-primary" style={{ fontSize: primarySize }}>{primary}</div>
			<div className="dual-unit-secondary" style={{ fontSize: `calc(${primarySize} * 0.75)` }}>{secondary}</div>
		</div>
	);
}
