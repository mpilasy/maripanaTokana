import DualUnitText from './DualUnitText';

interface DetailCardProps {
	title: string;
	value: string;
	secondaryValue?: string;
	subtitle?: string;
	onToggleUnits?: () => void;
}

export default function DetailCard({ title, value, secondaryValue, subtitle, onToggleUnits }: DetailCardProps) {
	return (
		<div className="detail-card">
			<span className="detail-card-title">{title}</span>
			{secondaryValue ? (
				<DualUnitText
					primary={value}
					secondary={secondaryValue}
					primarySize="20px"
					onClick={onToggleUnits}
				/>
			) : (
				<span className="detail-card-value">{value}</span>
			)}
			{subtitle && <span className="detail-card-subtitle">{subtitle}</span>}
		</div>
	);
}
